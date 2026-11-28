package dev.monarchui.components.overlays.lightbox

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.monarchui.tokens.MonarchTokens

/** Component tokens and the pure navigation and zoom rules of MonarchLightbox. */
internal object MonarchLightboxTokens {
    val Padding: Dp = MonarchTokens.Space.space4.dp
    val Gap: Dp = MonarchTokens.Space.space2.dp

    /** A caption taller than this scrolls inside its box. */
    val CaptionMaxHeight: Dp = MonarchTokens.Space.space24.dp

    /** The zoom buttons and the keys change the zoom by this much, as on the web. */
    const val ZOOM_STEP: Float = 0.5f

    /** A double tap on a fitted image zooms to this level. */
    const val DOUBLE_TAP_ZOOM: Float = 2f

    /** The index shown for [selected]: the nearest item, or 0 when there are none. */
    fun clampIndex(selected: Int, count: Int): Int = selected.coerceIn(0, maxOf(count - 1, 0))

    /**
     * The item [delta] steps from [index], wrapping when [looping], or
     * `null` when there is no such item.
     */
    fun step(index: Int, delta: Int, count: Int, looping: Boolean): Int? {
        if (count < 2) return null
        val raw = index + delta
        val next = if (looping) Math.floorMod(raw, count) else raw
        return next.takeIf { it in 0 until count && it != index }
    }

    /** The largest zoom: [maximumZoom], and at least 1, which turns zoom off. */
    fun zoomLimit(maximumZoom: Float): Float = if (maximumZoom.isNaN()) 1f else maxOf(1f, maximumZoom)

    /** [zoom] kept between 1 and [limit]. */
    fun clampZoom(zoom: Float, limit: Float): Float = zoom.coerceIn(1f, limit)

    /** The zoom a double tap goes to: in when fitted, back to fitted otherwise. */
    fun doubleTapZoom(zoom: Float, limit: Float): Float = if (zoom > 1f) 1f else minOf(DOUBLE_TAP_ZOOM, limit)

    /**
     * [offset] kept so that the zoomed image still covers its stage of
     * [size]: at most half the extra width and height in each direction.
     */
    fun clampOffset(offset: Offset, zoom: Float, size: IntSize): Offset {
        val maxX = size.width * (zoom - 1f) / 2f
        val maxY = size.height * (zoom - 1f) / 2f
        return Offset(offset.x.coerceIn(-maxX, maxX), offset.y.coerceIn(-maxY, maxY))
    }
}
