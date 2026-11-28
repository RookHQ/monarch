package com.rookhq.monarch.components.datagrid.cursorlist

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.R
import com.rookhq.monarch.components.datagrid.loadmore.MonarchLoadMore
import com.rookhq.monarch.components.datagrid.loadmore.MonarchLoadMoreLabels
import com.rookhq.monarch.components.datagrid.loadmore.MonarchLoadMoreTokens
import com.rookhq.monarch.components.feedback.emptystate.MonarchEmptyState
import com.rookhq.monarch.components.feedback.emptystate.MonarchEmptyStateSize
import com.rookhq.monarch.components.feedback.errorstate.MonarchErrorState
import com.rookhq.monarch.components.feedback.loadingstate.MonarchLoadingState
import com.rookhq.monarch.components.feedback.loadingstate.MonarchLoadingStateVariant
import com.rookhq.monarch.components.layout.divider.MonarchDivider
import com.rookhq.monarch.internal.monarchString

/**
 * A list loaded page by page from a cursor: the items, a Load more footer
 * with a count, and the first-load, empty and error states.
 *
 * Built by Monarch from a `LazyColumn` with a `MonarchLoadMore` as its last
 * item; with no items it shows a `MonarchLoadingState`, a `MonarchErrorState`
 * or a `MonarchEmptyState` instead. Fetching stays in the app (a view model,
 * or Paging's `LazyPagingItems`): the list shows the items it is given and
 * calls [onLoadMore]. After a page asked for from the footer arrives, focus
 * moves to its first new item, so people using a keyboard or TalkBack carry
 * on where the new page starts.
 *
 * The list scrolls itself: give it a bounded height (`Modifier.weight(1f)`
 * or `Modifier.height(…)`), and do not put it in a vertically scrolling
 * parent.
 *
 * @param T the type of the items.
 * @param items every item loaded so far, in order.
 * @param label the TalkBack name of the list.
 * @param hasMore whether there is another page. With no items and no more, the empty state is shown.
 * @param onLoadMore loads the next page.
 * @param modifier the modifier of the list.
 * @param key a stable key per item, as for `LazyColumn`. `null` uses the position.
 * @param loading a page is being fetched. With no items, the placeholder is shown; otherwise the footer shows progress.
 * @param failed the last fetch failed. With no items, the error state is shown; otherwise the footer shows the failure.
 * @param totalCount the total, when the server knows it: "Showing 20 of 140 items".
 * @param autoLoad loads the next page when the footer scrolls into view. The button stays.
 * @param showEnd shows "That's everything" after the last page.
 * @param divided draws a divider between rows.
 * @param focusNewItems moves focus to the first item of a page asked for from the footer.
 * @param loadingVariant the shape of the first-page placeholder.
 * @param state the scroll state of the list.
 * @param contentPadding padding around the rows and the footer.
 * @param onRetry repeats the failed request. `null` calls [onLoadMore].
 * @param empty replaces the empty state, for example with a `MonarchEmptyState` with a first-run action.
 * @param labels replacements for the built-in strings, including the footer's.
 * @param row draws one item.
 */
@Suppress("LongParameterList")
@Composable
public fun <T> MonarchCursorList(
    items: List<T>,
    label: String,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
    key: ((item: T) -> Any)? = null,
    loading: Boolean = false,
    failed: Boolean = false,
    totalCount: Int? = null,
    autoLoad: Boolean = false,
    showEnd: Boolean = true,
    divided: Boolean = true,
    focusNewItems: Boolean = true,
    loadingVariant: MonarchLoadingStateVariant = MonarchLoadingStateVariant.List,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(),
    onRetry: (() -> Unit)? = null,
    empty: (@Composable () -> Unit)? = null,
    labels: MonarchCursorListLabels = MonarchCursorListLabels(),
    row: @Composable LazyItemScope.(item: T) -> Unit,
) {
    val tokens = MonarchCursorListTokens
    val retry = onRetry ?: onLoadMore
    val count = items.size
    // The first index of a page asked for from the footer, until it arrives.
    var pendingFrom by remember { mutableStateOf<Int?>(null) }
    var focusIndex by remember { mutableStateOf<Int?>(null) }
    val requester = remember { FocusRequester() }
    LaunchedEffect(count, loading) {
        val target = tokens.focusTarget(pendingFrom, count, loading) ?: run {
            if (!loading) pendingFrom = null
            return@LaunchedEffect
        }
        pendingFrom = null
        focusIndex = target
        state.scrollToItem(target)
        withFrameNanos { }
        requester.requestFocus()
    }
    val fromFooter: (() -> Unit) -> () -> Unit = { action ->
        {
            pendingFrom = if (focusNewItems) count else null
            action()
        }
    }
    when (tokens.body(count, hasMore, loading, failed)) {
        CursorListBody.Error -> MonarchErrorState(
            title = monarchString(labels.errorTitle, R.string.monarch_cursorlist_error_title),
            modifier = modifier,
            size = MonarchEmptyStateSize.Sm,
            retrying = loading,
            onRetry = retry,
            retryLabel = monarchString(labels.retry, R.string.monarch_cursorlist_retry),
        )
        CursorListBody.Loading -> MonarchLoadingState(
            label = monarchString(labels.loading, R.string.monarch_cursorlist_loading),
            modifier = modifier,
            variant = loadingVariant,
            rows = tokens.PLACEHOLDER_ROWS,
        )
        CursorListBody.Empty -> if (empty != null) {
            empty()
        } else {
            MonarchEmptyState(
                title = monarchString(labels.emptyTitle, R.string.monarch_cursorlist_empty_title),
                modifier = modifier,
                size = MonarchEmptyStateSize.Sm,
                message = monarchString(labels.emptyMessage, R.string.monarch_cursorlist_empty_message),
            )
        }
        CursorListBody.Items -> CursorListItems(
            items = items,
            label = label,
            modifier = modifier,
            key = key,
            state = state,
            contentPadding = contentPadding,
            divided = divided,
            focusIndex = focusIndex,
            onFocusLeft = { focusIndex = null },
            requester = requester,
            row = row,
        ) {
            CursorListFooter(
                hasMore = hasMore,
                onLoadMore = fromFooter(onLoadMore),
                onAutoLoad = onLoadMore,
                loading = loading,
                failed = failed,
                autoLoad = autoLoad,
                showEnd = showEnd,
                count = count,
                totalCount = totalCount,
                onRetry = fromFooter(retry),
                labels = labels,
            )
        }
    }
}

/** The rows and the footer in a `LazyColumn`; only the row at [focusIndex] can take focus. */
@Suppress("LongParameterList")
@Composable
private fun <T> CursorListItems(
    items: List<T>,
    label: String,
    modifier: Modifier,
    key: ((item: T) -> Any)?,
    state: LazyListState,
    contentPadding: PaddingValues,
    divided: Boolean,
    focusIndex: Int?,
    onFocusLeft: () -> Unit,
    requester: FocusRequester,
    row: @Composable LazyItemScope.(item: T) -> Unit,
    footer: @Composable () -> Unit,
) {
    LazyColumn(
        modifier = modifier.semantics { contentDescription = label },
        state = state,
        contentPadding = contentPadding,
    ) {
        itemsIndexed(items, key = key?.let { k -> { _, item -> k(item) } }) { index, item ->
            if (divided && index > 0) MonarchDivider()
            if (index == focusIndex) {
                // Only the item focus is moved to can take focus, until focus leaves it, as on the web.
                var focused by remember { mutableStateOf(false) }
                Box(
                    Modifier
                        .focusRequester(requester)
                        .onFocusChanged {
                            if (it.hasFocus) focused = true else if (focused) onFocusLeft()
                        }
                        .focusable(),
                ) { row(item) }
            } else {
                row(item)
            }
        }
        item(key = CursorListFooterKey.Footer) { footer() }
    }
}

/** The key of the footer item: an enum, so it can be saved in a Bundle and no app key equals it. */
private enum class CursorListFooterKey { Footer }

/**
 * The footer: a `MonarchLoadMore` whose button records that the page was
 * asked for from the footer. Auto-loading is done here, so a page loaded on
 * its own does not move focus.
 */
@Suppress("LongParameterList")
@Composable
private fun CursorListFooter(
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    onAutoLoad: () -> Unit,
    loading: Boolean,
    failed: Boolean,
    autoLoad: Boolean,
    showEnd: Boolean,
    count: Int,
    totalCount: Int?,
    onRetry: () -> Unit,
    labels: MonarchCursorListLabels,
) {
    val currentAutoLoad by rememberUpdatedState(onAutoLoad)
    LaunchedEffect(autoLoad, hasMore, loading, failed) {
        if (MonarchLoadMoreTokens.autoLoads(autoLoad, hasMore, loading, failed)) currentAutoLoad()
    }
    MonarchLoadMore(
        hasMore = hasMore,
        onLoadMore = onLoadMore,
        loading = loading,
        failed = failed,
        showEnd = showEnd,
        loadedCount = count,
        totalCount = totalCount,
        onRetry = onRetry,
        labels = footerLabels(labels),
    )
}

/** The footer's strings, with the list's retry label when only that one is given. */
private fun footerLabels(labels: MonarchCursorListLabels): MonarchLoadMoreLabels {
    val footer = labels.footer
    if (labels.retry == null || footer.retry != null) return footer
    return MonarchLoadMoreLabels(
        loadMore = footer.loadMore,
        loading = footer.loading,
        failed = footer.failed,
        retry = labels.retry,
        end = footer.end,
        count = footer.count,
        loaded = footer.loaded,
    )
}
