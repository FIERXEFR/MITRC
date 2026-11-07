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

class SplashActivity : ComponentActivity() {

    /**
     * Kicked off in [onCreate] so the `admin_db` lookup runs alongside the 2.6s splash
     * animation instead of adding to it. Defaults to "not an admin", which preserves the
     * original routing whenever the lookup fails or never finishes.
     */
    private val adminCheck = CompletableDeferred<Boolean>()

    /** Set when the app was opened by tapping a push notification. */
    private var openNotifications = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        readNotificationIntent(intent)

        lifecycleScope.launch {
            val uid = runCatching { AuthRepository.currentUser?.uid }.getOrNull()
            val email = runCatching { AuthRepository.currentUser?.email }.getOrNull()
            adminCheck.complete(if (uid == null) false else AdminRepository.isAdmin(uid, email))
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
    }

    private fun readNotificationIntent(source: Intent?) {
        if (source?.getBooleanExtra(EXTRA_OPEN_NOTIFICATIONS, false) == true) {
            openNotifications = true
        }
    }

    private fun openApp() {
        if (isFinishing) return

        val loggedIn = try {
            AuthRepository.isLoggedIn
        } catch (error: Throwable) {
            false
        }

        lifecycleScope.launch {
            if (loggedIn) {
                // The splash animation already gave the admin lookup its full 1.5s window.
                // Never hold the splash open longer for a network call.
                val isAdmin = if (adminCheck.isCompleted) adminCheck.await() else false
                if (isFinishing) return@launch
                if (isAdmin) {
                    startActivity(Intent(this@SplashActivity, AdminPanelActivity::class.java))
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
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
                    // If the portal is already running underneath, reuse it and
                    // deliver the extra through onNewIntent instead of stacking.
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
            )
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }
    }

    companion object {
        const val EXTRA_OPEN_NOTIFICATIONS = "extra_open_notifications"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }
}
