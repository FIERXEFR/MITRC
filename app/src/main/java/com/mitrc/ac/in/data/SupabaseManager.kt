package com.mitrc.ac.`in`.data

import android.util.Base64
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.mitrc.ac.`in`.utils.NativeUtils
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.github.jan.supabase.storage.Storage
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.Json
import org.json.JSONObject
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

object SupabaseManager {

    private const val TAG = "SupabaseManager"

    private fun serializer(): KotlinXSerializer = KotlinXSerializer(
        Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
    )

    private const val USER_WAIT_ATTEMPTS = 4
    private const val USER_WAIT_DELAY_MS = 250L

    @Volatile
    private var client: SupabaseClient? = null

    @Volatile
    private var adminClient: SupabaseClient? = null

    private val coldStartLock = Mutex()

    @Volatile
    private var coldStartWaitDone = false

    private val roleRefreshAttemptedUids: MutableSet<String> =
        Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())

    val isConfigured: Boolean
        get() = client != null

    @OptIn(SupabaseInternal::class)
    fun init() {
        if (client != null) return
        synchronized(this) {
            if (client != null) return

            val url = readSecret("supabaseUrl") { NativeUtils.getSupabaseUrl() }
            val anonKey = readSecret("supabaseAnonKey") { NativeUtils.getSupabaseAnonKey() }
            if (url.isEmpty() || anonKey.isEmpty()) {
                Log.e(TAG, "Supabase not configured: a native secret could not be read.")
                return
            }
            if (url.startsWith("https://PROJECT-REF") || anonKey.startsWith("PASTE_")) {
                Log.e(TAG, "Supabase not configured: placeholder credentials.")
                return
            }

            client = createSupabaseClient(supabaseUrl = url, supabaseKey = anonKey) {
                defaultSerializer = serializer()
                accessToken = { currentIdToken() }
                install(Postgrest)
                install(Storage)
                httpEngine = OkHttp.create()
                httpConfig {
                    install(createClientPlugin("mitrc-firebase-token") {
                        onRequest { request, _ ->
                            val token = currentIdToken()
                            if (!token.isNullOrBlank()) {
                                request.headers[HttpHeaders.Authorization] = "Bearer $token"
                            }
                        }
                    })
                }
            }
            Log.i(TAG, "Supabase configured for $url")
        }
    }

    private suspend fun currentIdToken(): String? {
        val auth = FirebaseAuth.getInstance()
        auth.currentUser?.let { return idTokenFor(it) }

        return coldStartLock.withLock {
            val userNow = auth.currentUser
            if (userNow != null) return@withLock idTokenFor(userNow)
            if (coldStartWaitDone) return@withLock null

            var found: FirebaseUser? = null
            for (attempt in 0 until USER_WAIT_ATTEMPTS) {
                found = auth.currentUser
                if (found != null) break
                if (attempt < USER_WAIT_ATTEMPTS - 1) delay(USER_WAIT_DELAY_MS)
            }
            coldStartWaitDone = true
            found?.let { idTokenFor(it) }
        }
    }

    private suspend fun idTokenFor(user: FirebaseUser): String? {
        var token = runCatching { user.getIdToken(false).await()?.token }
            .onFailure { error ->
                Log.w(TAG, "getIdToken failed for uid=${user.uid}: ${error.message}")
                if (isAuthFailure(error)) {
                    Log.w(TAG, "Cached auth credential is invalid or expired. Signing out stale session.")
                    runCatching { FirebaseAuth.getInstance().signOut() }
                }
            }
            .getOrNull()

        if (token.isNullOrBlank()) {
            Log.w(TAG, "getIdToken returned no token for uid=${user.uid}")
            return null
        }

        if (!hasAuthenticatedRole(token) && roleRefreshAttemptedUids.add(user.uid)) {
            Log.w(
                TAG,
                "Firebase ID token for uid=${user.uid} has no role=authenticated claim; " +
                    "forcing one refresh so Supabase does not fall back to the anon role"
            )
            token = runCatching { user.getIdToken(true).await()?.token }
                .onFailure { Log.w(TAG, "Forced ID-token refresh failed for uid=${user.uid}", it) }
                .getOrNull()
            if (!token.isNullOrBlank() && !hasAuthenticatedRole(token)) {
                Log.e(
                    TAG,
                    "Firebase user uid=${user.uid} still has no role=authenticated claim. " +
                        "Assign it with the Firebase Admin SDK or a blocking auth function."
                )
            }
        }

        return token?.takeIf { it.isNotBlank() }
    }

    private fun hasAuthenticatedRole(token: String): Boolean = runCatching {
        val payload = token.split('.').getOrNull(1) ?: return@runCatching false
        val decoded = Base64.decode(
            payload,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
        JSONObject(String(decoded, Charsets.UTF_8)).optString("role") == "authenticated"
    }.getOrDefault(false)

    suspend fun refreshIdToken(): Boolean = runCatching {
        FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.await()?.token != null
    }.getOrElse {
        Log.w(TAG, "Forced token refresh failed", it)
        false
    }

    private val authFailureMarkers = listOf(
        "jwt", "pgrst301", "pgrst302", "expired", "unauthorized", "invalid claim",
        "invalid token", "not authorized", "supplied auth credential", "credential", "malformed"
    )

    private fun isAuthFailure(error: Throwable): Boolean {
        var current: Throwable? = error
        var depth = 0
        while (current != null && depth < 6) {
            val message = current.message.orEmpty().lowercase()
            if (authFailureMarkers.any { marker -> message.contains(marker) }) return true
            current = current.cause
            depth++
        }
        return false
    }

    suspend fun <T> withAuthRetry(block: suspend () -> T): T = try {
        block()
    } catch (error: Throwable) {
        if (!isAuthFailure(error) || !isConfigured) throw error
        Log.w(TAG, "Auth failure, refreshing token and retrying once", error)
        if (!refreshIdToken()) throw error
        block()
    }

    private inline fun readSecret(label: String, read: () -> String): String = try {
        read().trim()
    } catch (error: Throwable) {
        Log.e(TAG, "Native read of $label failed. Check that native-lib.cpp exports " +
            "Java_com_mitrc_ac_in_utils_NativeUtils_* for package com.mitrc.ac.in.utils.", error)
        ""
    }

    suspend fun getAuthTokenInfo(): String {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        if (user == null) {
            return "NO_FIREBASE_USER (Using Supabase Anon Key)"
        }
        val token = runCatching { user.getIdToken(false).await()?.token }.getOrNull()
        return if (!token.isNullOrBlank()) {
            "FIREBASE_ID_TOKEN_PRESENT (uid=${user.uid}, email=${user.email}, tokenLength=${token.length}, prefix=${token.take(12)}...)"
        } else {
            "FIREBASE_USER_EXISTS_BUT_TOKEN_NULL (Using Supabase Anon Key)"
        }
    }

    fun requireClient(): SupabaseClient =
        client ?: error("Supabase is not configured. Run scripts/generate_xor_cpp.ps1 with real credentials.")

    fun supabaseUrl(): String = readSecret("supabaseUrl") { NativeUtils.getSupabaseUrl() }

    fun supabaseAnonKey(): String =
        readSecret("supabaseAnonKey") { NativeUtils.getSupabaseAnonKey() }

    fun requireAdminClient(): SupabaseClient {
        adminClient?.let { return it }
        synchronized(this) {
            adminClient?.let { return it }

            val url = readSecret("supabaseUrl") { NativeUtils.getSupabaseUrl() }
            val serviceKey = readSecret("supabaseServiceKey") { NativeUtils.getSupabaseServiceKey() }
            if (url.isEmpty() || serviceKey.isEmpty()) {
                error(
                    "The coordinator write key is unavailable. Regenerate the native secrets with " +
                        "scripts/generate_xor_cpp.ps1, then rebuild the app."
                )
            }

            val created = createSupabaseClient(supabaseUrl = url, supabaseKey = serviceKey) {
                defaultSerializer = serializer()
                install(Postgrest)
                httpEngine = OkHttp.create()
            }
            Log.i(TAG, "Service-role client configured for coordinator writes")
            adminClient = created
            return created
        }
    }
}
