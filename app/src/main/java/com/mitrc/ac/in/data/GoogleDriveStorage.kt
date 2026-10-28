package com.mitrc.ac.`in`.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.concurrent.TimeUnit

/**
 * Uploads files to the account-wide Google Drive bucket through the `drive-storage` Edge
 * Function. Google credentials live only in Supabase function secrets - the app authenticates
 * with its Firebase ID token and never handles the client secret or refresh token.
 *
 * Uploads are resumable: the function creates a Drive upload session and the bytes go straight
 * from the phone to Google, so large PDFs are not proxied or held in function memory.
 *
 * Drive is blob storage, not a database: persist the returned [StoredFile.url] in Supabase
 * (`notes.drive_url`, `notifications.image_url`, ...) and read it back from there.
 */
object GoogleDriveStorage {

    private const val TAG = "GoogleDriveStorage"
    private const val FUNCTION_NAME = "drive-storage"

    /** Hard ceiling for a single upload; keeps the resumable PUT's body out of OOM territory. */
    private const val MAX_UPLOAD_BYTES = 50L * 1024 * 1024

    data class StoredFile(
        val fileId: String,
        val url: String,
        val viewUrl: String? = null,
        val downloadUrl: String? = null,
        val imageUrl: String? = null,
        val name: String? = null,
        val mimeType: String? = null,
        val size: Long? = null
    )

    private val json = Json { ignoreUnknownKeys = true }

    private val client by lazy {
        HttpClient(OkHttp) {
            expectSuccess = false
            engine {
                config {
                    connectTimeout(30, TimeUnit.SECONDS)
                    readTimeout(180, TimeUnit.SECONDS)
                    writeTimeout(180, TimeUnit.SECONDS)
                }
            }
        }
    }

    /**
     * Uploads [uri] into [folder] (a path under the bucket root, e.g. "Notes", "Events",
     * "Profiles"). The function creates missing folders and shares the file as "anyone with
     * the link" when [makePublic] is true.
     */
    suspend fun upload(
        context: Context,
        uri: Uri,
        folder: String,
        fileName: String? = null,
        mimeType: String? = null,
        makePublic: Boolean = true
    ): Result<StoredFile> = runCatching {
        val resolver = context.contentResolver
        val detectedType = resolver.getType(uri)
        val type = mimeType
            ?: detectedType?.takeIf { it.isNotBlank() && it != "application/octet-stream" }
            ?: guessMimeType(uri)
        val name = sanitizeName(
            fileName ?: resolver.displayName(uri) ?: "upload_${System.currentTimeMillis()}"
        )

        // Read once into memory for the resumable PUT. Refuse absurdly large files instead of
        // risking an OutOfMemoryError on a low-end phone.
        val declaredSize = withContext(Dispatchers.IO) {
            runCatching { resolver.openAssetFileDescriptor(uri, "r")?.use { it.length } }
                .getOrNull() ?: -1L
        }
        require(declaredSize <= MAX_UPLOAD_BYTES) {
            "That file is too large. The limit is ${MAX_UPLOAD_BYTES / (1024 * 1024)} MB."
        }

        val bytes = withContext(Dispatchers.IO) {
            resolver.openInputStream(uri)?.use { it.readBytes() }
        } ?: error("The selected file could not be read.")

        require(bytes.size <= MAX_UPLOAD_BYTES) {
            "That file is too large. The limit is ${MAX_UPLOAD_BYTES / (1024 * 1024)} MB."
        }

        uploadBytes(
            folder = folder,
            fileName = name,
            mimeType = type,
            bytes = bytes,
            makePublic = makePublic
        ).getOrThrow()
    }.onFailure { Log.w(TAG, "Drive upload failed for folder=$folder", it) }

    suspend fun uploadBytes(
        folder: String,
        fileName: String,
        mimeType: String,
        bytes: ByteArray,
        makePublic: Boolean = true
    ): Result<StoredFile> = runCatching {
        val ticket = post(
            UploadRequest(
                action = "create_upload",
                path = folder,
                fileName = fileName,
                mimeType = mimeType,
                size = bytes.size.toLong(),
                makePublic = makePublic
            )
        )

        val uploadUrl = ticket.uploadUrl
            ?: error("Drive did not return an upload session.")
        Log.i(TAG, "Uploading ${bytes.size} bytes to Drive folder=$folder as $fileName")

        val uploadResponse = client.put(uploadUrl) {
            contentType(ContentType.parse(mimeType))
            setBody(bytes)
        }
        val uploadText = uploadResponse.bodyAsText()
        if (!uploadResponse.status.isSuccess()) {
            error("Drive upload failed (${uploadResponse.status}): ${uploadText.take(300)}")
        }

        val fileId = runCatching {
            json.parseToJsonElement(uploadText).jsonObject["id"]?.jsonPrimitive?.content
        }.getOrNull() ?: error("Drive did not return the uploaded file id.")

        val stored = post(
            UploadRequest(
                action = "finalize",
                fileId = fileId,
                makePublic = makePublic
            )
        )
        stored.toStoredFile()
    }.onFailure { Log.w(TAG, "Drive upload failed for $fileName", it) }

    suspend fun delete(fileId: String): Result<Unit> = runCatching {
        post(UploadRequest(action = "delete", fileId = fileId))
        Unit
    }

    suspend fun list(folder: String): Result<List<StoredFile>> = runCatching {
        // `list` returns an array, which the shared flat response model does not cover.
        val token = firebaseToken()
        val response = client.post(endpoint()) {
            header(HttpHeaders.Authorization, "Bearer $token")
            header("apikey", SupabaseManager.supabaseAnonKey())
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(UploadRequest(action = "list", path = folder)))
        }
        val text = response.bodyAsText()
        if (!response.status.isSuccess()) error(errorMessage(text, response.status.value))

        val files = json.parseToJsonElement(text).jsonObject["files"]
            ?: return@runCatching emptyList()
        json.decodeFromJsonElement(
            kotlinx.serialization.builtins.ListSerializer(StoredFileDto.serializer()),
            files
        ).map { it.toStoredFile() }
    }

    /** Verifies the Google credentials and returns the bucket's root folder id. */
    suspend fun health(): Result<String> = runCatching {
        val response = post(UploadRequest(action = "health"))
        val root = response.rootFolderId ?: error("Drive health check returned no root folder id.")
        Log.i(TAG, "Drive bucket root=$root")
        root
    }

    // ---------------------------------------------------------------------------------------

    private suspend fun post(request: UploadRequest): FunctionResponse {
        val token = firebaseToken()
        val response = client.post(endpoint()) {
            header(HttpHeaders.Authorization, "Bearer $token")
            header("apikey", SupabaseManager.supabaseAnonKey())
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(request))
        }

        val text = response.bodyAsText()
        if (!response.status.isSuccess()) error(errorMessage(text, response.status.value))

        val parsed = runCatching {
            json.decodeFromString<FunctionResponse>(text)
        }.getOrElse { error("Unexpected drive-storage response: ${text.take(300)}") }

        if (parsed.error != null) error(parsed.error)
        return parsed
    }

    private fun errorMessage(text: String, status: Int): String = runCatching {
        json.parseToJsonElement(text).jsonObject["error"]?.jsonPrimitive?.content
    }.getOrNull() ?: "Upload failed ($status)"

    private suspend fun firebaseToken(): String {
        val user = FirebaseAuth.getInstance().currentUser
            ?: error("Sign in again to upload files.")
        return user.getIdToken(false).await()?.token
            ?: error("Could not obtain a Firebase token. Sign in again.")
    }

    private fun endpoint(): String {
        val base = SupabaseManager.supabaseUrl().trim().trimEnd('/')
        if (base.isEmpty()) error("Supabase URL is not configured.")
        return "$base/functions/v1/$FUNCTION_NAME"
    }

    private fun ContentResolver.displayName(uri: Uri): String? = runCatching {
        query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull()

    private fun sanitizeName(raw: String): String {
        val cleaned = raw.trim().replace('/', '-').replace('\\', '-')
        return cleaned.ifEmpty { "upload_${System.currentTimeMillis()}" }
    }

    private fun guessMimeType(uri: Uri): String = when (
        uri.lastPathSegment?.substringAfterLast('.', "")?.lowercase()
    ) {
        "pdf" -> "application/pdf"
        "png" -> "image/png"
        "webp" -> "image/webp"
        "gif" -> "image/gif"
        "jpg", "jpeg" -> "image/jpeg"
        "doc" -> "application/msword"
        "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        "ppt" -> "application/vnd.ms-powerpoint"
        "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        "xls" -> "application/vnd.ms-excel"
        "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        "zip" -> "application/zip"
        "txt" -> "text/plain"
        "mp4" -> "video/mp4"
        else -> "application/octet-stream"
    }

    // ---------------------------------------------------------------------------------------

    @Serializable
    private data class UploadRequest(
        val action: String,
        val path: String? = null,
        val fileName: String? = null,
        val mimeType: String? = null,
        val size: Long? = null,
        val makePublic: Boolean = true,
        val fileId: String? = null
    )

    @Serializable
    private data class FunctionResponse(
        val ok: Boolean = false,
        val error: String? = null,
        val fileId: String? = null,
        val name: String? = null,
        val mimeType: String? = null,
        val size: Long? = null,
        val url: String? = null,
        val viewUrl: String? = null,
        val downloadUrl: String? = null,
        val imageUrl: String? = null,
        val uploadUrl: String? = null,
        val folderId: String? = null,
        val rootFolderId: String? = null
    ) {
        fun toStoredFile(): StoredFile {
            val id = fileId ?: error("Drive returned no file id.")
            return StoredFile(
                fileId = id,
                url = url ?: viewUrl ?: error("Drive returned no link for $id."),
                viewUrl = viewUrl,
                downloadUrl = downloadUrl,
                imageUrl = imageUrl,
                name = name,
                mimeType = mimeType,
                size = size
            )
        }
    }

    /** Same shape as [FunctionResponse], used when decoding the `files` array. */
    @Serializable
    private data class StoredFileDto(
        val fileId: String,
        val name: String? = null,
        val mimeType: String? = null,
        val size: Long? = null,
        val url: String? = null,
        val viewUrl: String? = null,
        val downloadUrl: String? = null,
        val imageUrl: String? = null
    ) {
        fun toStoredFile(): StoredFile = StoredFile(
            fileId = fileId,
            url = url ?: viewUrl ?: downloadUrl ?: "",
            viewUrl = viewUrl,
            downloadUrl = downloadUrl,
            imageUrl = imageUrl,
            name = name,
            mimeType = mimeType,
            size = size
        )
    }
}
