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

    /**
     * Kicked off in [onCreate] so the `admin_db` lookup runs alongside the 2.6s splash
     * animation instead of adding to it. Defaults to "not an admin", which preserves the
     * original routing whenever the lookup fails or never finishes.
     */
    private val adminCheck = CompletableDeferred<Boolean>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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

    private fun openApp() {
        if (isFinishing) return

        val loggedIn = try {
            AuthRepository.isLoggedIn
        } catch (error: Throwable) {
            false
        }

        lifecycleScope.launch {
            if (loggedIn) {
                // Bounded so a dead network can never strand the user on the splash screen.
                val isAdmin = withTimeoutOrNull(ADMIN_CHECK_TIMEOUT_MS) { adminCheck.await() } ?: false
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
                }
            )
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }
    }

    private companion object {
        const val ADMIN_CHECK_TIMEOUT_MS = 4_000L
    }
}
