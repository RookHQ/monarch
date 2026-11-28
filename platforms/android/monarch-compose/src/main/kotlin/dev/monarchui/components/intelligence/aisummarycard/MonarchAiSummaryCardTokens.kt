package dev.monarchui.components.intelligence.aisummarycard

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** What the body of the card shows. */
internal enum class MonarchAiSummaryCardBody { Summary, Placeholder, Nothing, Empty }

/** Component tokens and logic of MonarchAiSummaryCard, built from primitives and roles only. */
internal object MonarchAiSummaryCardTokens {
    val Gap: Dp = MonarchTokens.Space.space3.dp
    val TitleGap: Dp = MonarchTokens.Space.space1.dp
    val HeaderGap: Dp = MonarchTokens.Space.space2.dp
    val PlaceholderGap: Dp = MonarchTokens.Space.space2.dp

    /** The widths of the placeholder lines, as fractions of the card, as on the web. */
    val PlaceholderLines: List<Float> = listOf(1f, 0.92f, 0.64f)

    /** Refresh is busy while a refresh runs or a summary is generating. */
    fun busy(status: MonarchAiSummaryCardStatus, refreshing: Boolean): Boolean =
        refreshing || status == MonarchAiSummaryCardStatus.Generating

    /** The summary when there is one, a placeholder while busy, nothing after an error, else the empty text. */
    fun body(summary: String, busy: Boolean, status: MonarchAiSummaryCardStatus): MonarchAiSummaryCardBody = when {
        summary.isNotBlank() -> MonarchAiSummaryCardBody.Summary
        busy -> MonarchAiSummaryCardBody.Placeholder
        status == MonarchAiSummaryCardStatus.Error -> MonarchAiSummaryCardBody.Nothing
        else -> MonarchAiSummaryCardBody.Empty
    }

    /**
     * What to announce when busy changes: [generating] when it starts,
     * [updated] when it ends (nothing after an error), `null` when nothing
     * changed, so nothing is announced when the card first appears.
     */
    fun announcement(
        wasBusy: Boolean,
        busy: Boolean,
        status: MonarchAiSummaryCardStatus,
        generating: String,
        updated: String,
    ): String? = when {
        busy && !wasBusy -> generating
        !busy && wasBusy -> if (status == MonarchAiSummaryCardStatus.Error) "" else updated
        else -> null
    }

    fun titleColor(colors: MonarchColors): Color = colors.textDefault

    fun mutedColor(colors: MonarchColors): Color = colors.textMuted
}
