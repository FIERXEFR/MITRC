package com.mitrc.ac.`in`.notifications

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * A one-shot flag that carries "the user tapped a push" across the
 * Splash -> MainActivity -> portal boundary.
 *
 * Compose observes [pending], so a tap that arrives while the portal is already
 * open still opens the notifications sheet.
 */
object NotificationOpenRequest {

    var pending by mutableStateOf(false)
        private set

    fun request() {
        pending = true
    }

    fun consume() {
        pending = false
    }
}
