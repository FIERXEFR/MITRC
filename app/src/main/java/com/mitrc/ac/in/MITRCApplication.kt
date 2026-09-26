package com.mitrc.ac.`in`

import android.app.Application
import com.mitrc.ac.`in`.data.SupabaseManager

class MITRCApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SupabaseManager.init()
    }
}
