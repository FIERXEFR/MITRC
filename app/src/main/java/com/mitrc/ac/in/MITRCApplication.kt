package com.mitrc.ac.`in`

import android.app.Application
import com.mitrc.ac.`in`.auth.AdminRepository
import com.mitrc.ac.`in`.data.AppUpdateStore
import com.mitrc.ac.`in`.data.GreetingStore
import com.mitrc.ac.`in`.data.OnboardingStore
import com.mitrc.ac.`in`.data.PasswordResetStore
import com.mitrc.ac.`in`.data.SupabaseManager
import com.mitrc.ac.`in`.notifications.NotificationChannels

class MITRCApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationChannels.ensure(this)
        OnboardingStore.init(this)
        GreetingStore.init(this)
        PasswordResetStore.init(this)
        AppUpdateStore.init(this)
        SupabaseManager.init()
        AdminRepository.init(this)
    }
}
