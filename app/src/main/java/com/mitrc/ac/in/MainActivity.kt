package com.mitrc.ac.`in`

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mitrc.ac.`in`.ui.screens.HomeScreen
import com.mitrc.ac.`in`.ui.screens.LoginScreen
import com.mitrc.ac.`in`.ui.theme.MITRCTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val startLoggedIn = intent.getBooleanExtra(EXTRA_START_LOGGED_IN, false)
        setContent {
            MITRCTheme {
                MitrcRoot(startLoggedIn = startLoggedIn)
            }
        }
    }

    companion object {
        const val EXTRA_START_LOGGED_IN = "extra_start_logged_in"
        const val ROUTE_LOGIN = "login"
        const val ROUTE_HOME = "home"
    }
}

@Composable
fun MitrcRoot(
    startLoggedIn: Boolean,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = if (startLoggedIn) MainActivity.ROUTE_HOME else MainActivity.ROUTE_LOGIN
    ) {
        composable(MainActivity.ROUTE_LOGIN) {
            LoginScreen(
                onLoggedIn = {
                    navController.navigate(MainActivity.ROUTE_HOME) {
                        popUpTo(MainActivity.ROUTE_LOGIN) { inclusive = true }
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
