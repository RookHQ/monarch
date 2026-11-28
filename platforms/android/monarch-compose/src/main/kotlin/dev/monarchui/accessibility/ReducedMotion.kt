package dev.monarchui.accessibility

import android.content.ContentResolver
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/** Whether a system animator duration scale means that animations are off. */
internal fun isReducedMotionScale(animatorDurationScale: Float): Boolean = animatorDurationScale == 0f

private fun ContentResolver.isReducedMotion(): Boolean =
    isReducedMotionScale(Settings.Global.getFloat(this, Settings.Global.ANIMATOR_DURATION_SCALE, 1f))

/**
 * Whether the user has turned animations off ("Remove animations" in the
 * accessibility settings, or an animator duration scale of 0 in the developer
 * options). Follows changes while in composition.
 *
 * `MonarchTheme` reads this once and exposes it as `MonarchTheme.motion.reducedMotion`.
 */
@Composable
public fun isReducedMotionEnabled(): Boolean {
    val resolver = LocalContext.current.contentResolver
    var reduced by remember(resolver) { mutableStateOf(resolver.isReducedMotion()) }
    DisposableEffect(resolver) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                reduced = resolver.isReducedMotion()
            }
        }
        val uri = Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE)
        resolver.registerContentObserver(uri, false, observer)
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    return reduced
}
