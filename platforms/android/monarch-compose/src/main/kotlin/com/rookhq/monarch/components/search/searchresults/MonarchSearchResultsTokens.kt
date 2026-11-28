package com.rookhq.monarch.components.search.searchresults

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens
import java.text.NumberFormat
import java.util.Locale

/** What MonarchSearchResults shows. */
internal enum class MonarchSearchResultsStatus { Ready, Refreshing, Loading, Empty, Error }

/** Component tokens and the pure logic of MonarchSearchResults. */
internal object MonarchSearchResultsTokens {
    /** The space between the summary, the groups and the rows of a skeleton. */
    val Gap: Dp = MonarchTokens.Space.space2.dp

    /** The space above a group heading. */
    val GroupGap: Dp = MonarchTokens.Space.space4.dp

    /** The space at the start and end of the summary, headings and skeleton rows, lined up with the rows. */
    val InlinePadding: Dp = MonarchTokens.Space.space4.dp

    /** The size of a group heading's icon. */
    val HeadingIconSize: Dp = MonarchTokens.Space.space4.dp

    /** The width of the first, shorter skeleton line of a loading row, as a fraction of the row. */
    const val SKELETON_TITLE_FRACTION: Float = 0.4f

    /** The width of the second skeleton line of a loading row, as a fraction of the row. */
    const val SKELETON_LINE_FRACTION: Float = 0.85f

    /** The colour of the summary and the group counts. */
    fun mutedColor(colors: MonarchColors): Color = colors.textMuted

    /** The colour of a group heading. */
    fun headingColor(colors: MonarchColors): Color = colors.textDefault

    /** Groups that have results; empty groups are left out. */
    fun visibleGroups(groups: List<MonarchSearchResultGroup>): List<MonarchSearchResultGroup> =
        groups.filter { it.results.isNotEmpty() }

    /** A group's total: its own total, never fewer than the results it shows. */
    fun groupTotal(group: MonarchSearchResultGroup): Int = maxOf(group.total ?: 0, group.results.size)

    /** The total of every visible group. */
    fun total(groups: List<MonarchSearchResultGroup>): Int = visibleGroups(groups).sumOf { groupTotal(it) }

    /** Whether a group offers Show all: the app handles it and the group has more than it shows. */
    fun showsAll(group: MonarchSearchResultGroup, canShowAll: Boolean): Boolean =
        canShowAll && group.title != null && groupTotal(group) > group.results.size

    /** What to show: an error wins over results, and results win over loading. */
    fun status(failed: Boolean, hasResults: Boolean, loading: Boolean): MonarchSearchResultsStatus = when {
        failed -> MonarchSearchResultsStatus.Error
        hasResults && loading -> MonarchSearchResultsStatus.Refreshing
        hasResults -> MonarchSearchResultsStatus.Ready
        loading -> MonarchSearchResultsStatus.Loading
        else -> MonarchSearchResultsStatus.Empty
    }

    /** The query as it is named in the summary and the empty title: trimmed, so only spaces is no query. */
    fun shownQuery(query: String): String = query.trim()

    /** A count in the digits and grouping of [locale]. */
    fun count(value: Int, locale: Locale): String = NumberFormat.getIntegerInstance(locale).format(value)
}
