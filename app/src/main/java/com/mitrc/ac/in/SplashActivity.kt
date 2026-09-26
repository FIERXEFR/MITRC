package com.mitrc.ac.`in`

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mitrc.ac.`in`.auth.AuthRepository
import com.mitrc.ac.`in`.ui.screens.SplashContent
import com.mitrc.ac.`in`.ui.theme.MITRCTheme

class SplashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_START_LOGGED_IN, loggedIn)
            }
        )
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
}
