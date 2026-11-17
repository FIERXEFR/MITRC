package com.mitrc.ac.`in`

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.mitrc.ac.`in`.auth.AdminRepository
import com.mitrc.ac.`in`.auth.AuthRepository
import com.mitrc.ac.`in`.data.OnboardingStore
import com.mitrc.ac.`in`.ui.screens.SplashContent
import com.mitrc.ac.`in`.ui.theme.MITRCTheme
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class SplashActivity : ComponentActivity() {

    private val adminCheck = CompletableDeferred<Boolean>()

    private var openNotifications = false

    private var isNavigating = false

    private var skipSplash = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        readNotificationIntent(intent)
        skipSplash = savedInstanceState == null &&
            intent.getBooleanExtra(EXTRA_SKIP_SPLASH, false)

        lifecycleScope.launch {
            val uid = runCatching { AuthRepository.currentUser?.uid }.getOrNull()
            val email = runCatching { AuthRepository.currentUser?.email }.getOrNull()
            adminCheck.complete(if (uid == null) false else AdminRepository.isAdmin(uid, email))
        }

        if (skipSplash) {
            openApp()
            return
        }

        setContent {
            MITRCTheme(darkTheme = true) {
                SplashContent(onFinished = ::openApp)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readNotificationIntent(intent)
        if (intent.getBooleanExtra(EXTRA_SKIP_SPLASH, false)) {
            skipSplash = true
            openApp()
        }
    }

    private fun readNotificationIntent(source: Intent?) {
        if (source == null) return
        if (source.getBooleanExtra(EXTRA_OPEN_NOTIFICATIONS, false) ||
            source.hasExtra("google.message_id") ||
            source.hasExtra(EXTRA_NOTIFICATION_ID)
        ) {
            openNotifications = true
        }
    }

    private fun openApp() {
        if (isFinishing || isNavigating) return
        isNavigating = true

        val loggedIn = try {
            AuthRepository.isLoggedIn
        } catch (error: Throwable) {
            false
        }

        lifecycleScope.launch {
            if (loggedIn) {
                val isAdmin = withTimeoutOrNull(1_000) { adminCheck.await() } ?: false
                if (isFinishing) return@launch
                if (isAdmin) {
                    startActivity(Intent(this@SplashActivity, AdminPanelActivity::class.java))
                    applyTransition()
                    finish()
                    return@launch
                }
            }

            if (isFinishing) return@launch
            val startDestination = when {
                loggedIn -> MainActivity.ROUTE_HOME
                OnboardingStore.isCompleted -> MainActivity.ROUTE_LOGIN
                else -> MainActivity.ROUTE_ONBOARDING
            }
            startActivity(
                Intent(this@SplashActivity, MainActivity::class.java).apply {
                    putExtra(MainActivity.EXTRA_START_DESTINATION, startDestination)
                    if (openNotifications) {
                        putExtra(MainActivity.EXTRA_OPEN_NOTIFICATIONS, true)
                    }
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
            )
            applyTransition()
            finish()
        }
    }

    private fun applyTransition() {
        if (skipSplash) {
            overridePendingTransition(0, 0)
        } else {
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    companion object {
        const val EXTRA_OPEN_NOTIFICATIONS = "extra_open_notifications"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_SKIP_SPLASH = "extra_skip_splash"
    }
}
