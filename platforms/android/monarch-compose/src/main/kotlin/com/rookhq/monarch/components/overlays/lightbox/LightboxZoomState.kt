package com.rookhq.monarch.components.overlays.lightbox

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animate
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import java.text.NumberFormat

/**
 * The zoom of the item on screen. Zoom belongs to one item: the viewer
 * resets it when another item is shown.
 *
 * @property limit the largest zoom; 1 turns zoom off.
 */
@Stable
internal class LightboxZoomState(val limit: Float) {
    var zoom: Float by mutableFloatStateOf(1f)
        private set
    var offset: Offset by mutableStateOf(Offset.Zero)
        private set
    var stageSize: IntSize by mutableStateOf(IntSize.Zero)

    val zoomable: Boolean get() = limit > 1f

    /** Applies a pinch and a pan from a gesture, at once. */
    fun transform(zoomChange: Float, pan: Offset) {
        zoom = MonarchLightboxTokens.clampZoom(zoom * zoomChange, limit)
        offset = MonarchLightboxTokens.clampOffset(offset + pan, zoom, stageSize)
    }

    /** Moves to [target] with [spec]; the spec is an immediate change when animations are removed. */
    suspend fun animateTo(target: Float, spec: AnimationSpec<Float>) {
        val end = MonarchLightboxTokens.clampZoom(target, limit)
        animate(zoom, end, animationSpec = spec) { value, _ ->
            zoom = value
            offset = MonarchLightboxTokens.clampOffset(offset, value, stageSize)
        }
    }

    /** Back to the fitted image, at once. */
    fun reset() {
        zoom = 1f
        offset = Offset.Zero
    }
}

/** The zoom level as a percentage in the user's locale, for example "150%". */
internal fun zoomPercent(zoom: Float): String = NumberFormat.getPercentInstance().format(zoom.toDouble())
