package com.rookhq.monarch.components.datagrid.loadmore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.internal.MonarchLiveStatus
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme

/**
 * The footer of a paginated list: a Load more button with loading, failure
 * and retry, the end of the list, a count and polite announcements.
 *
 * Built by Monarch from a `Column` with a `MonarchButton`: Material 3 has no
 * equivalent. The app keeps the cursor and the pages and tells the footer
 * where it stands. When the count grows, "20 more items loaded." is
 * announced; a failure and the end of the list are announced too, through a
 * polite live region. With [autoLoad] the next page loads as soon as the
 * footer is composed, which inside a `LazyColumn` means when it scrolls into
 * view; the button stays for people who do not scroll, and auto-loading
 * pauses after a failure.
 *
 * @param hasMore whether there is another page. `false` shows the end line.
 * @param onLoadMore loads the next page. Not called while [loading].
 * @param modifier the modifier of the footer.
 * @param loading a page is being fetched: the button shows progress and ignores taps.
 * @param failed the last fetch failed: the failure is shown, the button becomes Retry and auto-loading pauses.
 * @param autoLoad loads the next page when the footer is composed. Put the footer in a lazy list.
 * @param showEnd shows and announces "That's everything" at the end.
 * @param loadedCount the items on screen. Shown in the count; an increase is announced.
 * @param totalCount the total, when the server knows it.
 * @param showCount shows the count. `null` shows it when [loadedCount] is given.
 * @param onRetry called by Retry after a failure. `null` calls [onLoadMore].
 * @param labels replacements for the built-in strings.
 */
@Composable
public fun MonarchLoadMore(
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    failed: Boolean = false,
    autoLoad: Boolean = false,
    showEnd: Boolean = true,
    loadedCount: Int? = null,
    totalCount: Int? = null,
    showCount: Boolean? = null,
    onRetry: (() -> Unit)? = null,
    labels: MonarchLoadMoreLabels = MonarchLoadMoreLabels(),
) {
    val colors = MonarchTheme.colors
    val state = MonarchLoadMoreTokens.state(hasMore, loading, failed)
    val failedText = monarchString(labels.failed, R.string.monarch_loadmore_failed)
    val endText = monarchString(labels.end, R.string.monarch_loadmore_end)
    val currentLoad by rememberUpdatedState(onLoadMore)
    LaunchedEffect(autoLoad, hasMore, loading, failed) {
        if (MonarchLoadMoreTokens.autoLoads(autoLoad, hasMore, loading, failed)) currentLoad()
    }
    val announcement = loadMoreAnnouncement(loadedCount, failed, hasMore, showEnd, failedText, endText, labels)
    Box(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = MonarchLoadMoreTokens.VerticalPadding),
            verticalArrangement = Arrangement.spacedBy(MonarchLoadMoreTokens.Gap),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (MonarchLoadMoreTokens.showsCount(showCount, loadedCount) && loadedCount != null) {
                Text(
                    text = countText(loadedCount, totalCount, labels),
                    style = MonarchTheme.typography.caption,
                    color = MonarchLoadMoreTokens.mutedText(colors),
                )
            }
            if (failed) {
                Text(
                    text = failedText,
                    style = MonarchTheme.typography.label,
                    color = MonarchLoadMoreTokens.failedText(colors),
                )
            }
            if (state == LoadMoreState.Done) {
                if (showEnd) {
                    Text(
                        text = endText,
                        style = MonarchTheme.typography.caption,
                        color = MonarchLoadMoreTokens.mutedText(colors),
                    )
                }
            } else {
                MonarchButton(
                    text = if (failed) {
                        monarchString(labels.retry, R.string.monarch_loadmore_retry)
                    } else {
                        monarchString(labels.loadMore, R.string.monarch_loadmore_load_more)
                    },
                    onClick = { if (failed) (onRetry ?: onLoadMore)() else onLoadMore() },
                    size = MonarchButtonSize.Sm,
                    loading = loading && !failed,
                    loadingLabel = monarchString(labels.loading, R.string.monarch_loadmore_loading),
                )
            }
        }
        MonarchLiveStatus(text = announcement.takeIf { it.isNotEmpty() }, visible = false)
    }
}

@Composable
private fun countText(loaded: Int, total: Int?, labels: MonarchLoadMoreLabels): String =
    labels.count?.invoke(loaded, total) ?: if (total != null) {
        pluralStringResource(R.plurals.monarch_loadmore_count_of, total, loaded, total)
    } else {
        pluralStringResource(R.plurals.monarch_loadmore_count, loaded, loaded)
    }

/** The values the announcement compares against: those of the last change. */
private class LoadMoreSeen(var count: Int?, var failed: Boolean, var hasMore: Boolean)

/**
 * The text of the polite live region after a change: the items a page added,
 * a new failure and the end of the list. Nothing on the first composition.
 */
@Suppress("LongParameterList")
@Composable
private fun loadMoreAnnouncement(
    loadedCount: Int?,
    failed: Boolean,
    hasMore: Boolean,
    showEnd: Boolean,
    failedText: String,
    endText: String,
    labels: MonarchLoadMoreLabels,
): String {
    var announcement by remember { mutableStateOf("") }
    val seen = remember { LoadMoreSeen(loadedCount, failed, hasMore) }
    val added = MonarchLoadMoreTokens.added(seen.count, loadedCount)
    val loadedText = if (added > 0) {
        labels.loaded?.invoke(added) ?: pluralStringResource(R.plurals.monarch_loadmore_loaded, added, added)
    } else {
        null
    }
    val parts = listOfNotNull(
        loadedText,
        failedText.takeIf { failed && !seen.failed },
        endText.takeIf { showEnd && !hasMore && seen.hasMore },
    )
    val separator = stringResource(R.string.monarch_loadmore_announcement_separator)
    LaunchedEffect(loadedCount, failed, hasMore) {
        seen.count = loadedCount
        seen.failed = failed
        seen.hasMore = hasMore
        if (parts.isNotEmpty()) {
            announcement = ""
            withFrameNanos { }
            announcement = parts.joinToString(separator)
        }
    }
    return announcement
}
