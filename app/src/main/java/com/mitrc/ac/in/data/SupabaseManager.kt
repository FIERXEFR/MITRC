package com.mitrc.ac.`in`.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.mitrc.ac.`in`.utils.NativeUtils
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

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

    val isConfigured: Boolean
        get() = client != null

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
            }
            Log.i(TAG, "Supabase configured for $url")
        }
    }

    /**
     * A live Firebase ID token for Supabase.
     *
     * Waits out the cold-start window described on [USER_WAIT_ATTEMPTS] and only gives up (and
     * returns `null`, which makes Supabase fall back to the anon key) once no user appears.
     */
    private suspend fun currentIdToken(): String? {
        val auth = FirebaseAuth.getInstance()
        repeat(USER_WAIT_ATTEMPTS) { attempt ->
            val user = auth.currentUser
            if (user != null) {
                val token = runCatching { user.getIdToken(false).await()?.token }.getOrNull()
                if (!token.isNullOrBlank()) return token
                Log.w(TAG, "getIdToken returned no token for uid=${user.uid}, attempt=$attempt")
            }
            if (attempt < USER_WAIT_ATTEMPTS - 1) delay(USER_WAIT_DELAY_MS)
        }
        Log.w(TAG, "No signed-in user for a Supabase request after $USER_WAIT_ATTEMPTS attempts")
        return null
    }

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
        "invalid token", "not authorized"
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
}
