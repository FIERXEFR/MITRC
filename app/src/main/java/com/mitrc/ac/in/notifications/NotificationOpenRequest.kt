package com.mitrc.ac.`in`.notifications

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

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
