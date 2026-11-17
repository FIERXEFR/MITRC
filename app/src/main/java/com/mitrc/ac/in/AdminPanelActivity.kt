package com.mitrc.ac.`in`

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.mitrc.ac.`in`.auth.AuthRepository
import com.mitrc.ac.`in`.ui.components.OfflineDialog
import com.mitrc.ac.`in`.ui.components.rememberIsOnline
import com.mitrc.ac.`in`.ui.screens.AdminPanelScreen
import com.mitrc.ac.`in`.ui.theme.MITRCTheme

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
                Box(modifier = Modifier.fillMaxSize()) {
                    AdminPanelScreen(onSignedOut = ::openLogin)
                    if (!rememberIsOnline()) {
                        OfflineDialog()
                    }
                }
            }
        }
    }

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
