package com.rookhq.monarch.components.datagrid.loadmore

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** What the footer shows. */
internal enum class LoadMoreState { Idle, Loading, Failed, Done }

/** Component tokens and rules of MonarchLoadMore, built from primitives and roles only. */
internal object MonarchLoadMoreTokens {
    val Gap: Dp = MonarchTokens.Space.space2.dp
    val VerticalPadding: Dp = MonarchTokens.Space.space3.dp

    /** The state of the footer: a failure wins over loading, which wins over the end. */
    fun state(hasMore: Boolean, loading: Boolean, failed: Boolean): LoadMoreState = when {
        failed -> LoadMoreState.Failed
        loading -> LoadMoreState.Loading
        hasMore -> LoadMoreState.Idle
        else -> LoadMoreState.Done
    }

    /** Whether the count is shown: as asked, or by default when the app gives the items on screen. */
    fun showsCount(showCount: Boolean?, loadedCount: Int?): Boolean = loadedCount != null && (showCount ?: true)

    /** Whether the next page is loaded on its own now: asked for, there is more, and nothing runs or failed. */
    fun autoLoads(autoLoad: Boolean, hasMore: Boolean, loading: Boolean, failed: Boolean): Boolean =
        autoLoad && hasMore && !loading && !failed

    /** The number of items the latest page added, or 0 when the count did not grow. */
    fun added(previous: Int?, current: Int?): Int =
        if (previous != null && current != null && current > previous) current - previous else 0

    /** The count and the end line are muted text; the failure the strong danger colour. */
    fun mutedText(colors: MonarchColors): Color = colors.textMuted

    /** The colour of the failure message. */
    fun failedText(colors: MonarchColors): Color = colors.dangerStrong
}
