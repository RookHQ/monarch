package dev.monarchui.components.datadisplay.insightstrip

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.monarchui.foundation.MonarchTypography
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** Component tokens and rules of MonarchInsightStrip, built from primitives and roles only. */
internal object MonarchInsightStripTokens {
    val Gap: Dp = MonarchTokens.Space.space3.dp
    val CardGap: Dp = MonarchTokens.Space.space2.dp
    val BadgeGap: Dp = MonarchTokens.Space.space1_5.dp

    /** Between the title and the reason: 4dp, as on the web. */
    val ReasonGap: Dp = MonarchTokens.Space.space1.dp

    /** Above the action buttons: 12dp, as on the web. */
    val ActionsGap: Dp = MonarchTokens.Space.space3.dp

    /** The title: 16sp semibold, snug, like a card title. Always larger and heavier than the reason. */
    fun titleStyle(typography: MonarchTypography): TextStyle =
        typography.bodyStrong.copy(lineHeight = MonarchTokens.LineHeight.snug.em)

    /** The reason: 14sp regular, in the muted colour. */
    fun reasonStyle(typography: MonarchTypography): TextStyle =
        typography.body.copy(fontSize = MonarchTokens.FontSize.sm.sp)

    /** The narrowest a card is in the grid: 288dp, 18rem on the web. */
    val MinCardWidth: Dp = (MonarchTokens.Space.space24 * GRID_STEPS).dp

    /** The widest a card is in the scrolling row: 320dp, 20rem on the web. */
    val MaxCardWidth: Dp = (MonarchTokens.Space.space20 * ROW_STEPS).dp

    /** The height of a placeholder card while loading. It holds no text. */
    val PlaceholderHeight: Dp = MonarchTokens.Space.space24.dp

    /** In the scrolling row a card takes this fraction of the width, so the next one shows. */
    const val ROW_CARD_FRACTION: Float = 0.85f

    const val MAX_COLUMNS: Int = 3
    const val PLACEHOLDER_COUNT: Int = 3
    private const val GRID_STEPS = 3
    private const val ROW_STEPS = 4

    /** The fill of a placeholder card. */
    fun placeholderColor(colors: MonarchColors): Color = colors.bgMuted

    /** The colour of the empty message. */
    fun emptyTextColor(colors: MonarchColors): Color = colors.textMuted

    /** The tones most severe first: critical, warning, info, a note, positive. */
    private val SeverityOrder: List<MonarchTone> = listOf(
        MonarchTone.Danger,
        MonarchTone.Warning,
        MonarchTone.Info,
        MonarchTone.Neutral,
        MonarchTone.Success,
    )

    /** The rank of a tone, most severe first: critical, warning, info, a note, positive. */
    fun rank(tone: MonarchTone): Int = SeverityOrder.indexOf(tone)

    /** The insights in the order shown: most severe first, keeping the given order within a tone. */
    fun ordered(insights: List<MonarchInsight>, sortBySeverity: Boolean): List<MonarchInsight> =
        if (sortBySeverity) insights.sortedBy { rank(it.tone) } else insights

    /** Whether [limit] hides any of [count] insights. A limit of zero or less is ignored. */
    fun limits(limit: Int?, count: Int): Boolean = limit != null && limit > 0 && count > limit

    /** The insights shown: the first [limit] unless [expanded]. */
    fun shown(ordered: List<MonarchInsight>, limit: Int?, expanded: Boolean): List<MonarchInsight> =
        if (limit != null && limits(limit, ordered.size) && !expanded) ordered.take(limit) else ordered

    /** The number of grid columns for [width]: one per minimum card width, from one to three. */
    fun columns(width: Dp): Int = (width / MinCardWidth).toInt().coerceIn(1, MAX_COLUMNS)
}
