package dev.monarchui.components.files.avatarupload

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens
import kotlin.math.roundToInt

/**
 * The position of the picture in the crop area, in units of the area's side.
 *
 * @property zoom 1 when the short side of the image fills the area.
 * @property x the offset of the image centre from the area centre, positive to the right.
 * @property y the offset of the image centre from the area centre, positive downwards.
 */
internal data class AvatarUploadView(val zoom: Float = 1f, val x: Float = 0f, val y: Float = 0f)

/** Component tokens and rules of MonarchAvatarUpload, built from primitives and roles only. */
internal object MonarchAvatarUploadTokens {
    val Gap: Dp = MonarchTokens.Space.space3.dp
    val ButtonGap: Dp = MonarchTokens.Space.space2.dp

    /** The largest side of the crop area. */
    val CropAreaMax: Dp = (MonarchTokens.Space.space24 * CROP_AREA_STEPS).dp

    /** The width of the ring drawn around the kept area. */
    val RingWidth: Dp = MonarchTokens.Space.px.dp * 2

    const val MIN_ZOOM: Float = 1f
    const val MAX_ZOOM: Float = 4f

    /** One step of the move actions and arrow keys, in units of the area's side. */
    const val MOVE_STEP: Float = 0.04f

    /** One step of the zoom actions and plus and minus keys. */
    const val ZOOM_STEP: Float = 0.1f

    /** The output side when the app does not choose one, in pixels. */
    const val OUTPUT_SIZE: Int = 512

    /** Images are scaled down until their longest side is at most this, before cropping. */
    const val MAX_DECODE_SIDE: Int = 2048

    private const val CROP_AREA_STEPS = 3
    private const val PERCENT = 100
    private const val HALF = 0.5f

    /**
     * Keeps the view inside its limits: the zoom between [MIN_ZOOM] and
     * [MAX_ZOOM], and the offset so the image always covers the area.
     */
    fun clamp(width: Int, height: Int, view: AvatarUploadView): AvatarUploadView {
        val zoom = view.zoom.coerceIn(MIN_ZOOM, MAX_ZOOM)
        val short = minOf(width, height).coerceAtLeast(1).toFloat()
        val maxX = (width / short * zoom - 1f) / 2f
        val maxY = (height / short * zoom - 1f) / 2f
        return AvatarUploadView(zoom, view.x.coerceIn(-maxX, maxX), view.y.coerceIn(-maxY, maxY))
    }

    /** The square of the image (in its pixels) that a view shows. At zoom 1 the short side fills the area. */
    fun cropRect(width: Int, height: Int, view: AvatarUploadView): MonarchAvatarCrop {
        val size = minOf(width, height) / view.zoom
        return MonarchAvatarCrop(
            x = width / 2f - view.x * size - size / 2f,
            y = height / 2f - view.y * size - size / 2f,
            size = size,
        )
    }

    /**
     * Where the picture is drawn in an area of [side] pixels: its short side
     * fills the area at zoom 1, and its centre is offset by the view.
     */
    fun imageBox(width: Int, height: Int, view: AvatarUploadView, side: Float): Rect {
        val short = minOf(width, height).coerceAtLeast(1).toFloat()
        val w = width / short * view.zoom * side
        val h = height / short * view.zoom * side
        val centre = Offset((HALF + view.x) * side, (HALF + view.y) * side)
        return Rect(centre.x - w / 2f, centre.y - h / 2f, centre.x + w / 2f, centre.y + h / 2f)
    }

    /** The zoom as a whole percentage. */
    fun percent(zoom: Float): Int = (zoom * PERCENT).roundToInt()

    /** The fill outside the kept area: the canvas colour, so only what is kept is seen. */
    fun maskColor(colors: MonarchColors): Color = colors.bgCanvas

    /** The ring around the kept area. */
    fun ringColor(colors: MonarchColors): Color = colors.borderStrong

    /** The colour of an error message. */
    fun errorColor(colors: MonarchColors): Color = colors.dangerStrong

    /** The colour of the hint. */
    fun hintColor(colors: MonarchColors): Color = colors.textMuted
}
