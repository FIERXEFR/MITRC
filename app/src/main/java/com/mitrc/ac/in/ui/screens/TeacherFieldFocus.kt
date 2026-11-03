package com.mitrc.ac.`in`.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged

/**
 * Tracks whether any teacher-portal text field is focused so the shell can consume the first
 * system-back press to dismiss the keyboard instead of leaving the portal.
 */
internal class TeacherFieldFocusTracker {
    private var focusedKeys by mutableStateOf<Set<Any>>(emptySet())

    val hasFocusedField: Boolean get() = focusedKeys.isNotEmpty()

    fun update(key: Any, focused: Boolean) {
        focusedKeys = if (focused) focusedKeys + key else focusedKeys - key
    }

    fun clear() {
        focusedKeys = emptySet()
    }
}

internal val LocalTeacherFieldFocusTracker = staticCompositionLocalOf { TeacherFieldFocusTracker() }

@Composable
internal fun Modifier.trackTeacherFieldFocus(): Modifier {
    val tracker = LocalTeacherFieldFocusTracker.current
    val key = remember { Any() }

    DisposableEffect(key) {
        onDispose { tracker.update(key, false) }
    }

    return onFocusChanged { tracker.update(key, it.isFocused) }
}
