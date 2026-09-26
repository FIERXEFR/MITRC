package com.mitrc.ac.`in`.data

import com.mitrc.ac.`in`.utils.NativeUtils
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.ktor.client.engine.okhttp.OkHttp

object SupabaseManager {

    @Volatile
    private var client: SupabaseClient? = null

    val isConfigured: Boolean
        get() = client != null

    fun init() {
        if (client != null) return
        synchronized(this) {
            if (client != null) return
            val url = runCatching { NativeUtils.getSupabaseUrl() }.getOrNull() ?: return
            val anonKey = runCatching { NativeUtils.getSupabaseAnonKey() }.getOrNull() ?: return
            if (url.startsWith("https://PROJECT-REF") || anonKey.startsWith("PASTE_")) return

            client = createSupabaseClient(supabaseUrl = url, supabaseKey = anonKey) {
                install(Postgrest)
                install(Auth)
                install(Storage)
                httpEngine = OkHttp.create()
            }
        }
    }

    fun requireClient(): SupabaseClient =
        client ?: error("Supabase is not configured. Run scripts/generate_xor_cpp.ps1 with real credentials.")
}
