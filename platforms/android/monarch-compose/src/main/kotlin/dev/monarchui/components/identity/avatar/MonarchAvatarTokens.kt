package dev.monarchui.components.identity.avatar

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.monarchui.components.identity.presencedot.MonarchPresenceDotSize
import dev.monarchui.foundation.MonarchTypography
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** The fill and the text colour of an avatar without a picture. */
@Immutable
internal data class MonarchAvatarFallbackColors(val container: Color, val content: Color)

/** Component tokens and logic of MonarchAvatar and MonarchAvatarGroup, built from primitives and roles only. */
internal object MonarchAvatarTokens {
    /** The ring in the surface colour around each avatar of a group. */
    val RingWidth: Dp = MonarchTokens.Space.space0_5.dp

    /** How much of an avatar the next one in a group covers (a quarter, like the web). */
    const val OVERLAP_RATIO: Float = 0.25f

    /**
     * How much of an avatar the next one covers at [size]. A 20 or 24dp avatar is mostly initials, so the
     * next one covers only an eighth and the letters stay whole; larger avatars keep the quarter.
     */
    fun overlapRatio(size: MonarchAvatarSize): Float = when (size) {
        MonarchAvatarSize.Xs, MonarchAvatarSize.Sm -> SMALL_OVERLAP_RATIO
        else -> OVERLAP_RATIO
    }

    private const val SMALL_OVERLAP_RATIO: Float = 0.125f

    /**
     * How far the presence dot of a 20 or 24dp avatar moves out over the corner, as a share of its own size, so
     * it does not sit on the initials.
     */
    const val SMALL_DOT_OUTSET: Float = 0.4f

    /** How far the initials may shrink to fit. */
    const val INITIALS_MINIMUM_SCALE: Float = 0.5f

    /** The side of an avatar at the default font size. */
    fun side(size: MonarchAvatarSize): Dp = when (size) {
        MonarchAvatarSize.Xs -> MonarchTokens.Space.space5.dp
        MonarchAvatarSize.Sm -> MonarchTokens.Space.space6.dp
        MonarchAvatarSize.Md -> MonarchTokens.Space.space8.dp
        MonarchAvatarSize.Lg -> MonarchTokens.Space.space10.dp
        MonarchAvatarSize.Xl -> (MonarchTokens.Space.space12 + MonarchTokens.Space.space2).dp
    }

    /** The outline of an avatar. */
    fun shape(shape: MonarchAvatarShape, size: MonarchAvatarSize): Shape = when (shape) {
        MonarchAvatarShape.Circle -> MonarchShapeDefaults.Mark
        MonarchAvatarShape.Square -> RoundedCornerShape(squareRadius(size))
    }

    /** The corner radius of the square shape. */
    fun squareRadius(size: MonarchAvatarSize): Dp = when (size) {
        MonarchAvatarSize.Xs, MonarchAvatarSize.Sm -> MonarchTokens.Radius.sm.dp
        MonarchAvatarSize.Md, MonarchAvatarSize.Lg -> MonarchTokens.Radius.md.dp
        MonarchAvatarSize.Xl -> MonarchTokens.Radius.lg.dp
    }

    /** The outline of the ring around an avatar of a group: the avatar's shape grown by the ring. */
    fun ringShape(shape: MonarchAvatarShape, size: MonarchAvatarSize): Shape = when (shape) {
        MonarchAvatarShape.Circle -> MonarchShapeDefaults.Mark
        MonarchAvatarShape.Square -> RoundedCornerShape(squareRadius(size) + RingWidth)
    }

    /** The type role of the initials: semibold at every size, as on the web (600), about 40 % of the side. */
    fun initialsStyle(size: MonarchAvatarSize, typography: MonarchTypography): TextStyle = when (size) {
        MonarchAvatarSize.Xs -> typography.caption.copy(fontSize = MonarchTokens.FontSize.xs2.sp)
        MonarchAvatarSize.Sm -> typography.caption
        MonarchAvatarSize.Md -> typography.label
        MonarchAvatarSize.Lg -> typography.bodyStrong
        MonarchAvatarSize.Xl -> typography.subheading
    }.copy(fontWeight = FontWeight.SemiBold)

    /** The size of the presence dot. */
    fun dotSize(size: MonarchAvatarSize): MonarchPresenceDotSize = when (size) {
        MonarchAvatarSize.Xs, MonarchAvatarSize.Sm -> MonarchPresenceDotSize.Sm
        MonarchAvatarSize.Md -> MonarchPresenceDotSize.Md
        MonarchAvatarSize.Lg, MonarchAvatarSize.Xl -> MonarchPresenceDotSize.Lg
    }

    /**
     * The soft fallback colours of the web: a per-name hue at low saturation, a pale fill with a dark text in a
     * light theme and a deep fill with a pale text in a dark one.
     */
    private const val FILL_SATURATION: Float = 0.45f
    private const val FILL_LIGHTNESS: Float = 0.92f
    private const val TEXT_SATURATION: Float = 0.55f
    private const val TEXT_LIGHTNESS: Float = 0.28f
    private const val DARK_FILL_SATURATION: Float = 0.30f
    private const val DARK_FILL_LIGHTNESS: Float = 0.22f
    private const val DARK_TEXT_SATURATION: Float = 0.60f
    private const val DARK_TEXT_LIGHTNESS: Float = 0.85f

    fun hueColors(hue: Int, dark: Boolean): MonarchAvatarFallbackColors {
        val h = hue.toFloat()
        return if (dark) {
            MonarchAvatarFallbackColors(
                Color.hsl(h, DARK_FILL_SATURATION, DARK_FILL_LIGHTNESS),
                Color.hsl(h, DARK_TEXT_SATURATION, DARK_TEXT_LIGHTNESS),
            )
        } else {
            MonarchAvatarFallbackColors(
                Color.hsl(h, FILL_SATURATION, FILL_LIGHTNESS),
                Color.hsl(h, TEXT_SATURATION, TEXT_LIGHTNESS),
            )
        }
    }

    /** The colours of the "+N" tile of a group. */
    fun tileColors(colors: MonarchColors): MonarchAvatarFallbackColors =
        MonarchAvatarFallbackColors(colors.bgMuted, colors.textMuted)

    /** The colour of the ring around each avatar of a group. */
    fun ringColor(colors: MonarchColors): Color = colors.bgSurface
}
