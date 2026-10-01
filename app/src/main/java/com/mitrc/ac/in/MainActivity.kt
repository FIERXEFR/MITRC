package com.mitrc.ac.`in`

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mitrc.ac.`in`.ui.screens.HomeScreen
import com.mitrc.ac.`in`.ui.screens.LoginScreen
import com.mitrc.ac.`in`.ui.screens.OnboardingScreen
import com.mitrc.ac.`in`.ui.theme.MITRCTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val requestedStart = intent.getStringExtra(EXTRA_START_DESTINATION)
        val startDestination =
            if (requestedStart != null && requestedStart in validRoutes) requestedStart
            else ROUTE_LOGIN
        setContent {
            MITRCTheme {
                MitrcRoot(startDestination = startDestination)
            }
        }
    }

    companion object {
        const val EXTRA_START_DESTINATION = "extra_start_destination"
        const val ROUTE_ONBOARDING = "onboarding"
        const val ROUTE_LOGIN = "login"
        const val ROUTE_HOME = "home"

        private val validRoutes = setOf(ROUTE_ONBOARDING, ROUTE_LOGIN, ROUTE_HOME)
    }
}

@Composable
fun MitrcRoot(
    startDestination: String,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(
            route = MainActivity.ROUTE_ONBOARDING,
            // Keep the navy backdrop fully visible while the next screen fades in over it.
            exitTransition = { ExitTransition.None }
        ) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(MainActivity.ROUTE_LOGIN) {
                        popUpTo(MainActivity.ROUTE_ONBOARDING) { inclusive = true }
                    }
                }
            )
        }
        composable(
            route = MainActivity.ROUTE_LOGIN,
            enterTransition = {
                fadeIn(tween(450, easing = FastOutSlowInEasing))
            }
        ) {
            val context = LocalContext.current
            LoginScreen(
                onLoggedIn = {
                    navController.navigate(MainActivity.ROUTE_HOME) {
                        popUpTo(MainActivity.ROUTE_LOGIN) { inclusive = true }
                    }
                },
                onAdminLoggedIn = {
                    val activity = context as? Activity
                    if (activity != null) {
                        activity.startActivity(
                            Intent(activity, AdminPanelActivity::class.java)
                        )
                        activity.overridePendingTransition(
                            android.R.anim.fade_in,
                            android.R.anim.fade_out
                        )
                        // The panel becomes the task root, so an admin's back stack never
                        // exposes the student/co-ordinator portal underneath it.
                        activity.finish()
                    }
                }
            )
        }
        composable(MainActivity.ROUTE_HOME) {
            HomeScreen(
                onSignedOut = {
                    navController.navigate(MainActivity.ROUTE_LOGIN) {
                        popUpTo(MainActivity.ROUTE_HOME) { inclusive = true }
                    }
                }
            )
        }
    }
}
