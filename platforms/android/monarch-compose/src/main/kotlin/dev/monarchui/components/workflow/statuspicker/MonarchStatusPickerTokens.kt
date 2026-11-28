package dev.monarchui.components.workflow.statuspicker

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/**
 * The colours of a status glyph.
 *
 * @property mark the ring, the half fill and the disc.
 * @property cutout the check mark or cross inside a disc.
 */
@Immutable
internal data class StatusGlyphColors(val mark: Color, val cutout: Color)

/** Component tokens of MonarchStatusPicker, built from primitives and roles only. */
internal object MonarchStatusPickerTokens {
    val GlyphSize: Dp = (MonarchTokens.Space.space3 + MonarchTokens.Space.space0_5).dp
    val Gap: Dp = MonarchTokens.Space.space2.dp

    /** The glyph's drawing grid, as on the web: a 14-unit square. */
    const val GRID: Float = 14f
    const val CENTRE: Float = 7f
    const val RING_RADIUS: Float = 5.5f
    const val DISC_RADIUS: Float = 6f
    const val HALF_RADIUS: Float = 3.5f
    const val STROKE: Float = 1.5f
    const val DASH: Float = 2f
    const val DASH_GAP: Float = 1.6f

    /** The check mark of completed, `M4.2 7.2l1.9 1.9 3.7-3.9`. */
    val Check: List<Offset> = listOf(Offset(4.2f, 7.2f), Offset(6.1f, 9.1f), Offset(9.8f, 5.2f))

    /** The cross of cancelled, `M5 5l4 4M9 5l-4 4`. */
    val Cross: List<Pair<Offset, Offset>> = listOf(Offset(5f, 5f) to Offset(9f, 9f), Offset(9f, 5f) to Offset(5f, 9f))

    /**
     * The glyph's colours, as on the web: the subtle text colour for backlog
     * and cancelled, the muted text colour for not started, the strong warning
     * colour for in progress and the strong success colour for completed. The
     * mark inside a disc is the surface colour.
     */
    fun glyphColors(colors: MonarchColors, category: MonarchStatusCategory): StatusGlyphColors = StatusGlyphColors(
        mark = when (category) {
            MonarchStatusCategory.Backlog, MonarchStatusCategory.Cancelled -> colors.textSubtle
            MonarchStatusCategory.Unstarted -> colors.textMuted
            MonarchStatusCategory.Started -> colors.warningStrong
            MonarchStatusCategory.Completed -> colors.successStrong
        },
        cutout = colors.bgSurface,
    )

    /**
     * The menu groups: one per category, in category order, with the
     * statuses of that category in their given order. Categories without a
     * status are left out.
     */
    fun groups(statuses: List<MonarchStatusOption>): List<Pair<MonarchStatusCategory, List<MonarchStatusOption>>> =
        MonarchStatusCategory.entries
            .map { category -> category to statuses.filter { it.category == category } }
            .filter { (_, members) -> members.isNotEmpty() }
}
