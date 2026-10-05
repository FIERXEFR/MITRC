package com.mitrc.ac.`in`

import android.app.Application
import com.mitrc.ac.`in`.auth.AdminRepository
import com.mitrc.ac.`in`.data.GreetingStore
import com.mitrc.ac.`in`.data.OnboardingStore
import com.mitrc.ac.`in`.data.SupabaseManager

class MITRCApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        OnboardingStore.init(this)
        GreetingStore.init(this)
        SupabaseManager.init()
        // Reads the Firebase Web API key out of the generated google-services resources so the
        // admin panel can create accounts without hardcoding it.
        AdminRepository.init(this)
    }
}
