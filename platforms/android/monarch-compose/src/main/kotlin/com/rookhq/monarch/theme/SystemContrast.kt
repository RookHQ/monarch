package com.rookhq.monarch.theme

import android.app.UiModeManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * The system contrast level from which Monarch switches to its high-contrast
 * themes. Android reports 0 for standard, 0.5 for medium and 1 for high;
 * Monarch has one high-contrast palette, so medium and high both select it.
 */
internal const val HighContrastThreshold: Float = 0.5f

/** Whether a value of `UiModeManager.getContrast()` selects the high-contrast themes. */
internal fun isHighContrastLevel(contrast: Float): Boolean = contrast >= HighContrastThreshold

/**
 * Whether the user has asked the system for more contrast.
 *
 * Reads `UiModeManager.getContrast()` and follows changes while in
 * composition. The setting exists from Android 14 (API 34); on earlier
 * versions this returns `false`.
 */
@Composable
public fun isSystemInHighContrast(): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        isHighContrastLevel(systemContrast())
    } else {
        false
    }

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@Composable
private fun systemContrast(): Float {
    val context = LocalContext.current
    val uiModeManager = remember(context) { context.getSystemService(UiModeManager::class.java) }
    var contrast by remember(uiModeManager) { mutableFloatStateOf(uiModeManager?.contrast ?: 0f) }
    DisposableEffect(uiModeManager, context) {
        val listener = UiModeManager.ContrastChangeListener { contrast = it }
        uiModeManager?.addContrastChangeListener(context.mainExecutor, listener)
        onDispose { uiModeManager?.removeContrastChangeListener(listener) }
    }
    return contrast
}
