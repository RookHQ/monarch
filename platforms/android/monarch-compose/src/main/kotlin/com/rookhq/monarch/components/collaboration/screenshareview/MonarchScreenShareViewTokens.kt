package com.rookhq.monarch.components.collaboration.screenshareview

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.internal.MonarchSurfaceColors
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

/** Component tokens and zoom rules of MonarchScreenShareView, built from primitives and roles only. */
internal object MonarchScreenShareViewTokens {
    val BarPadding: Dp = MonarchTokens.Space.space2.dp
    val Gap: Dp = MonarchTokens.Space.space2.dp
    val NoticePadding: Dp = MonarchTokens.Space.space4.dp

    /** The shared picture is shown in a 16 by 9 area. */
    const val ASPECT_RATIO: Float = 16f / 9f

    /** The zoom steps; 1 fits the whole screen. The same steps as on the web and iOS. */
    val ZoomLevels: List<Float> = listOf(1f, 1.5f, 2f, 3f)

    const val MIN_ZOOM: Float = 1f
    const val MAX_ZOOM: Float = 3f
    private const val STEP_TOLERANCE = 0.000_001f
    private const val PERCENT = 100

    /** The frame: the surface with the default border. */
    fun frameColors(colors: MonarchColors): MonarchSurfaceColors =
        MonarchSurfaceColors(colors.bgSurface, colors.textDefault, colors.borderDefault)

    /** The picture area: the inverse surface, with muted inverse text while waiting. */
    fun viewportColors(colors: MonarchColors): MonarchSurfaceColors =
        MonarchSurfaceColors(colors.bgInverse, colors.textInverseMuted, null)

    /** The notice that replaces the person's own picture. */
    fun noticeColors(colors: MonarchColors): MonarchSurfaceColors =
        MonarchSurfaceColors(colors.bgSubtle, colors.textMuted, null)

    /** [zoom] kept between 1 and 3. */
    fun clamp(zoom: Float): Float = zoom.coerceIn(MIN_ZOOM, MAX_ZOOM)

    /** The next step above [zoom], or `null` at the largest. */
    fun zoomIn(zoom: Float): Float? = ZoomLevels.firstOrNull { it > zoom + STEP_TOLERANCE }

    /** The next step below [zoom], or `null` at fit. */
    fun zoomOut(zoom: Float): Float? = ZoomLevels.lastOrNull { it < zoom - STEP_TOLERANCE }

    /** A double tap zooms one step in, and from the largest step back to fit. */
    fun doubleTap(zoom: Float): Float = zoomIn(zoom) ?: MIN_ZOOM

    /** The zoom as a whole percentage. */
    fun percent(zoom: Float): Int = (zoom * PERCENT).roundToInt()

    /** The zoom as a percentage in [locale], "150%". */
    fun percentText(zoom: Float, locale: Locale = Locale.getDefault()): String =
        NumberFormat.getPercentInstance(locale).format(percent(zoom) / PERCENT.toDouble())

    /** [offset] kept so the zoomed picture never leaves an empty edge in an area of [size]. */
    fun clampOffset(offset: Offset, size: Size, zoom: Float): Offset {
        if (zoom <= MIN_ZOOM) return Offset.Zero
        val maxX = size.width * (zoom - 1f) / 2f
        val maxY = size.height * (zoom - 1f) / 2f
        return Offset(offset.x.coerceIn(-maxX, maxX), offset.y.coerceIn(-maxY, maxY))
    }
}
