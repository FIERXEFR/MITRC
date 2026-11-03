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
import io.github.jan.supabase.storage.Storage
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

object SupabaseManager {

    private const val TAG = "SupabaseManager"

    /**
     * FirebaseAuth restores the persisted user asynchronously after a cold start, so the first
     * token request of a session can briefly see a null `currentUser`. Waiting a few beats beats
     * sending the query with no Authorization header, which PostgREST rejects with a JWT error.
     */
    private const val USER_WAIT_ATTEMPTS = 4
    private const val USER_WAIT_DELAY_MS = 250L

    @Volatile
    private var client: SupabaseClient? = null

    /**
     * Service-role client for coordinator-only writes. It is created lazily, only after a screen
     * has verified the signed-in account through `course_coordinators`, and it never installs the
     * Auth plugin: supabase-kt then sends the service key as `Authorization: Bearer ...`, which
     * bypasses RLS for the timetable and student editors.
     */
    @Volatile
    private var adminClient: SupabaseClient? = null

    /**
     * Serializes the one-time cold-start user lookup. Several portal queries can start before
     * Firebase has restored the persisted session; without this lock each one would wait out its
     * own timeout and then send an anon request.
     */
    private val coldStartLock = Mutex()

    @Volatile
    private var coldStartWaitDone = false

    /**
     * Firebase users must carry `role: "authenticated"` for Supabase's third-party auth bridge to
     * map them to the `authenticated` Postgres role. Firebase's client SDK caches ID tokens, so an
     * older token minted before the claim was configured can keep the app on the `anon` role.
     * Force one refresh per UID when that happens.
     */
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
                accessToken = { currentIdToken() }
                install(Postgrest)
                install(Storage)
                httpEngine = OkHttp.create()
                // supabase-kt 3.1.1 exposes `accessToken` but its Postgrest plugin does not attach
                // it to outgoing requests. Without this interceptor PostgREST only sees the anon
                // `apikey`, so Firebase users are treated as `anon` and every RLS-protected
                // write/RPC fails with "permission denied" or an RLS violation.
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

    /**
     * A live Firebase ID token for Supabase.
     *
     * Waits out the cold-start window described on [USER_WAIT_ATTEMPTS] once, and only gives up
     * (returning `null`, which makes Supabase use the anon key) after no user appears. Later
     * requests return immediately when the app is signed out instead of paying the timeout again.
     */
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

    /**
     * Reads the JWT payload without verifying it. This is only used to decide whether Firebase's
     * cached token predates the `role: authenticated` custom claim; Supabase still verifies the
     * signature and claim before trusting the token.
     */
    private fun hasAuthenticatedRole(token: String): Boolean = runCatching {
        val payload = token.split('.').getOrNull(1) ?: return@runCatching false
        val decoded = Base64.decode(
            payload,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
        JSONObject(String(decoded, Charsets.UTF_8)).optString("role") == "authenticated"
    }.getOrDefault(false)

    /**
     * Throws away Firebase's cached ID token so the next [currentIdToken] round-trips a brand new
     * one. Used to recover from a request that failed with an expired/invalid JWT.
     */
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

    /** True when [error] (or anything it wraps) looks like a rejected/missing auth token. */
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

    /**
     * Runs [block] and, when it fails because the auth token was rejected, forces Firebase to mint
     * a fresh one and retries exactly once. Without this a token that expires mid-session strands
     * the user on the error screen until they force-quit the app.
     */
    suspend fun <T> withAuthRetry(block: suspend () -> T): T = try {
        block()
    } catch (error: Throwable) {
        if (!isAuthFailure(error) || !isConfigured) throw error
        Log.w(TAG, "Auth failure, refreshing token and retrying once", error)
        if (!refreshIdToken()) throw error
        block()
    }

    /**
     * Reads one obfuscated secret, turning the two ways this can go wrong into a visible log
     * line instead of a silent `return` that later surfaces as "Supabase is not configured".
     *
     * A `null` return means either `System.loadLibrary("mitrcnative")` failed or the JNI symbol
     * does not match `Java_com_mitrc_ac_in_utils_NativeUtils_<method>` - a package rename of
     * [NativeUtils] silently breaks the lookup because the name is only resolved at runtime.
     */
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

    /** Project URL for direct REST calls the SDK does not cover (e.g. Edge Functions). */
    fun supabaseUrl(): String = readSecret("supabaseUrl") { NativeUtils.getSupabaseUrl() }

    /** Anon key sent alongside Edge Function calls as the gateway's `apikey` header. */
    fun supabaseAnonKey(): String =
        readSecret("supabaseAnonKey") { NativeUtils.getSupabaseAnonKey() }

    /**
     * The privileged client used by the coordinator timetable and student editors.
     *
     * The key is read from [NativeUtils] at runtime and is never held in a Kotlin constant. Callers
     * must gate on coordinator rights before touching this, because the key itself has no RLS.
     */
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
                install(Postgrest)
                httpEngine = OkHttp.create()
            }
            Log.i(TAG, "Service-role client configured for coordinator writes")
            adminClient = created
            return created
        }
    }
}
