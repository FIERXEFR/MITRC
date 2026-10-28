package com.mitrc.ac.`in`.ui.components

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mitrc.ac.`in`.ui.theme.Gold
import com.mitrc.ac.`in`.ui.theme.GoldLight
import com.mitrc.ac.`in`.ui.theme.NavyDeep
import com.mitrc.ac.`in`.ui.theme.NavySoft
import com.mitrc.ac.`in`.ui.theme.SurfaceWhite
import com.mitrc.ac.`in`.ui.theme.TextPrimary
import com.mitrc.ac.`in`.ui.theme.TextSecondary
import kotlinx.coroutines.delay

private fun Context.hasInternet(): Boolean {
    val manager = getSystemService(ConnectivityManager::class.java) ?: return false
    val network = manager.activeNetwork ?: return false
    val capabilities = manager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}

/**
 * Tracks validated internet connectivity for the lifetime of the composition.
 * NET_CAPABILITY_VALIDATED matters: Wi-Fi without a captive-portal login is "connected"
 * but cannot reach Supabase, so it must count as offline.
 */
@Composable
fun rememberIsOnline(): Boolean {
    val context = LocalContext.current
    var online by remember { mutableStateOf(context.hasInternet()) }

    DisposableEffect(context) {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                online = context.hasInternet()
            }

            override fun onLost(network: Network) {
                online = context.hasInternet()
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                online = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            }
        }
        runCatching { manager?.registerDefaultNetworkCallback(callback) }
        onDispose { runCatching { manager?.unregisterNetworkCallback(callback) } }
    }

    // Safety net: some OEMs kill or delay connectivity broadcasts while the app is backgrounded.
    // While offline, re-probe once a second so the gate always lifts on its own - no button.
    LaunchedEffect(online) {
        if (online) return@LaunchedEffect
        while (true) {
            delay(1_000)
            if (context.hasInternet()) {
                online = true
                return@LaunchedEffect
            }
        }
    }

    return online
}

/**
 * Non-dismissible offline gate. It has no buttons and cannot be closed by back press or
 * outside tap; it disappears on its own the moment connectivity returns.
 */
@Composable
fun OfflineDialog() {
    // Swallow system back and the predictive-back gesture while the gate is up.
    BackHandler(enabled = true) { }
    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 36.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = SurfaceWhite,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(NavySoft, NavyDeep))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.WifiOff,
                            contentDescription = null,
                            tint = GoldLight,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    Text(
                        text = "No internet connection",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "This app needs a live connection.\nReconnect to continue - this screen closes automatically.",
                        color = TextSecondary,
                        fontSize = 13.5.sp,
                        lineHeight = 19.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(22.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            color = Gold,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Waiting for connection...",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
