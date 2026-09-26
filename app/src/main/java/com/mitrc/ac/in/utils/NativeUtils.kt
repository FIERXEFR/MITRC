package com.mitrc.ac.`in`.utils

object NativeUtils {
    init {
        System.loadLibrary("mitrcnative")
    }

    external fun getSupabaseUrl(): String
    external fun getSupabaseAnonKey(): String
    external fun getSupabaseServiceKey(): String
    external fun getAppName(): String
}
