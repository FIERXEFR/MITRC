package com.mitrc.ac.`in`.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.mitrc.ac.`in`.utils.NativeUtils
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.tasks.await

object SupabaseManager {

    private const val TAG = "SupabaseManager"

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
                accessToken = {
                    runCatching {
                        FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.await()?.token
                    }.getOrNull()
                }
                install(Postgrest)
                install(Storage)
                httpEngine = OkHttp.create()
            }
            Log.i(TAG, "Supabase configured for $url")
        }
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

    fun requireClient(): SupabaseClient =
        client ?: error("Supabase is not configured. Run scripts/generate_xor_cpp.ps1 with real credentials.")
}
