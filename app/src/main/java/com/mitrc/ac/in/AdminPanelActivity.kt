package com.mitrc.ac.`in`

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mitrc.ac.`in`.auth.AuthRepository
import com.mitrc.ac.`in`.ui.screens.AdminPanelScreen
import com.mitrc.ac.`in`.ui.theme.MITRCTheme

/**
 * The admin panel's own activity, so an admin's back stack is the panel itself rather than the
 * student/co-ordinator portal.
 *
 * Both entry points - [SplashActivity] for a returning session and the login flow in
 * [MainActivity] - only start this after [com.mitrc.ac.in.auth.AdminRepository.isAdmin] returned
 * true for the signed-in UID. [AuthRepository.currentUser] is re-checked here purely as a cheap
 * guard against a session that disappeared in between.
 */
class AdminPanelActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (AuthRepository.currentUser == null) {
            openLogin()
            return
        }

        enableEdgeToEdge()
        setContent {
            MITRCTheme {
                AdminPanelScreen(onSignedOut = ::openLogin)
            }
        }
    }

    /**
     * Signs out of the panel by dropping the whole task and coming back up on the login route,
     * so no admin screen is left underneath for back-press to find.
     */
    private fun openLogin() {
        if (isFinishing) return
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_START_DESTINATION, MainActivity.ROUTE_LOGIN)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
        )
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
}
