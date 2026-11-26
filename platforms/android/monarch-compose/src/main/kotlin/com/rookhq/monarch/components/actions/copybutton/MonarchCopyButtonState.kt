package com.rookhq.monarch.components.actions.copybutton

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The copy action and the copied state of a copy button.
 *
 * @property copied whether the last copy is recent, for `resetAfterMillis`.
 * @property copy writes the value to the clipboard, plays the confirm haptic and starts the copied state.
 */
internal class MonarchCopyButtonState(val copied: Boolean, val copy: () -> Unit)

@Composable
@Suppress("LongParameterList")
internal fun rememberMonarchCopyButtonState(
    value: String,
    clipLabel: String,
    sensitive: Boolean,
    resetAfterMillis: Long,
    onCopy: ((String) -> Unit)?,
): MonarchCopyButtonState {
    val clipboard = LocalClipboard.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var copied by remember { mutableStateOf(false) }
    var copies by remember { mutableIntStateOf(0) }
    LaunchedEffect(copies) {
        if (copies > 0 && resetAfterMillis > 0) {
            delay(resetAfterMillis)
            copied = false
        }
    }
    return MonarchCopyButtonState(copied) {
        scope.launch {
            clipboard.setClipEntry(ClipEntry(MonarchCopyButtonTokens.clip(clipLabel, value, sensitive)))
            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            copied = true
            copies++
            onCopy?.invoke(value)
        }
    }
}
