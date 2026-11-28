package com.rookhq.monarch.components.datagrid.previewrows

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** The number of rows per status. */
internal typealias PreviewRowsCounts = Map<MonarchPreviewRowStatus, Int>

/** Component tokens and rules of MonarchPreviewRows, built from primitives and roles only. */
internal object MonarchPreviewRowsTokens {
    val RowPadding: Dp = MonarchTokens.Space.space3.dp
    val RowGap: Dp = MonarchTokens.Space.space1_5.dp
    /** The cells of a row side by side before it wraps. */
    const val COLUMNS: Int = 3

    /** The narrowest third of a row that keeps an email address on one line. */
    val MinCellWidth: Dp = (MonarchTokens.Space.space24 + MonarchTokens.Space.space4).dp

    val ValueGap: Dp = MonarchTokens.Space.space2.dp

    /** The order of the filter tabs after All: the problems first. */
    val FilterOrder: List<MonarchPreviewRowStatus> = listOf(
        MonarchPreviewRowStatus.Error,
        MonarchPreviewRowStatus.Warning,
        MonarchPreviewRowStatus.Duplicate,
        MonarchPreviewRowStatus.Ready,
    )

    /** The number of rows per status, every status present. */
    fun counts(rows: List<MonarchPreviewRow>): PreviewRowsCounts {
        val counts = MonarchPreviewRowStatus.entries.associateWith { 0 }.toMutableMap()
        rows.forEach { counts[it.outcome] = counts.getValue(it.outcome) + 1 }
        return counts
    }

    /** The rows a filter shows: all of them for `null`. */
    fun shown(rows: List<MonarchPreviewRow>, filter: MonarchPreviewRowStatus?): List<MonarchPreviewRow> =
        if (filter == null) rows else rows.filter { it.outcome == filter }

    /** Whether the filter tabs are shown: when asked and some row is not ready, or a filter is chosen. */
    fun showsFilter(
        filterable: Boolean,
        counts: PreviewRowsCounts,
        filter: MonarchPreviewRowStatus?,
    ): Boolean = filterable &&
        (filter != null || counts.any { (status, n) -> status != MonarchPreviewRowStatus.Ready && n > 0 })

    /** The tabs after All: the statuses with rows, and the chosen one even when it has none. */
    fun tabs(counts: PreviewRowsCounts, filter: MonarchPreviewRowStatus?): List<MonarchPreviewRowStatus> =
        FilterOrder.filter { counts.getValue(it) > 0 || it == filter }

    /** The tone of a status badge. */
    fun tone(status: MonarchPreviewRowStatus): MonarchTone = when (status) {
        MonarchPreviewRowStatus.Ready -> MonarchTone.Success
        MonarchPreviewRowStatus.Warning -> MonarchTone.Warning
        MonarchPreviewRowStatus.Error -> MonarchTone.Danger
        MonarchPreviewRowStatus.Duplicate -> MonarchTone.Info
    }

    /** The tone of the count in a tab: problems are coloured, the rest neutral. */
    fun countTone(status: MonarchPreviewRowStatus?): MonarchTone = when (status) {
        MonarchPreviewRowStatus.Error -> MonarchTone.Danger
        MonarchPreviewRowStatus.Warning -> MonarchTone.Warning
        else -> MonarchTone.Neutral
    }

    /** The colour of a problem: the strong danger colour for an error, the strong warning colour for a warning. */
    fun issueColor(error: Boolean, colors: MonarchColors): Color =
        if (error) colors.dangerStrong else colors.warningStrong

    /** The colour of field names, the dash of a missing value and the note. */
    fun mutedColor(colors: MonarchColors): Color = colors.textMuted

    /** The colour of values. */
    fun valueColor(colors: MonarchColors): Color = colors.textDefault
}
