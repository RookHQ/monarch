package dev.monarchui.components.workflow.prioritypicker

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/**
 * The colours of a priority glyph.
 *
 * @property mark the tile, the filled bars and the dashes.
 * @property empty the outline of a bar that is not filled.
 * @property cutout the exclamation mark inside the urgent tile.
 */
@Immutable
internal data class PriorityGlyphColors(val mark: Color, val empty: Color, val cutout: Color)

/** Component tokens of MonarchPriorityPicker, built from primitives and roles only. */
internal object MonarchPriorityPickerTokens {
    val GlyphSize: Dp = (MonarchTokens.Space.space3 + MonarchTokens.Space.space0_5).dp
    val Gap: Dp = MonarchTokens.Space.space2.dp

    /** The glyph's drawing grid, as on the web: a 14-unit square. */
    const val GRID: Float = 14f
    const val TILE_INSET: Float = 1f
    const val TILE_RADIUS: Float = 3f
    const val MARK_STROKE: Float = 1.6f
    const val DASH_STROKE: Float = 1.5f
    const val EMPTY_STROKE: Float = 1f
    const val BAR_WIDTH: Float = 3f
    const val BAR_RADIUS: Float = 1f
    const val BAR_COUNT: Int = 3

    /** The exclamation mark: a line and a dot, `M7 3.8v4M7 10v.1`. */
    val MarkLine: Pair<Offset, Offset> = Offset(7f, 3.8f) to Offset(7f, 7.8f)
    /** The dot that marks the current level in the menu. */
    val DotSize: Dp = MonarchTokens.Space.space2.dp

    val MarkDot: Pair<Offset, Offset> = Offset(7f, 10f) to Offset(7f, 10.1f)

    /** The three dashes of no priority, `M2.5 7h2M6 7h2M9.5 7h2`. */
    val Dashes: List<Pair<Offset, Offset>> = listOf(2.5f, 6f, 9.5f).map { Offset(it, 7f) to Offset(it + 2f, 7f) }

    /** The top-left corner and height of bar [index] (0 is the shortest), on the web's 14-unit grid. */
    @Suppress("MagicNumber")
    fun bar(index: Int): Pair<Offset, Float> =
        Offset(1.5f + index * 4f, 9f - index * 3f) to (3f + index * 3f)

    /** How many bars are filled at [level]: three for high, two for medium, one for low. */
    @Suppress("MagicNumber")
    fun filledBars(level: MonarchPriorityLevel): Int = when (level) {
        MonarchPriorityLevel.High -> 3
        MonarchPriorityLevel.Medium -> 2
        MonarchPriorityLevel.Low -> 1
        MonarchPriorityLevel.Urgent, MonarchPriorityLevel.None -> 0
    }

    /**
     * The glyph's colours: the strong danger colour for urgent, the strong
     * warning colour for high, the default text colour for medium and low and
     * the muted text colour for no priority. Empty bars use the strong border
     * colour instead of a faded mark.
     */
    fun glyphColors(colors: MonarchColors, level: MonarchPriorityLevel): PriorityGlyphColors = PriorityGlyphColors(
        mark = when (level) {
            MonarchPriorityLevel.Urgent -> colors.dangerStrong
            MonarchPriorityLevel.High -> colors.warningStrong
            MonarchPriorityLevel.Medium, MonarchPriorityLevel.Low -> colors.textDefault
            MonarchPriorityLevel.None -> colors.textMuted
        },
        empty = colors.borderStrong,
        cutout = colors.bgSurface,
    )
}
