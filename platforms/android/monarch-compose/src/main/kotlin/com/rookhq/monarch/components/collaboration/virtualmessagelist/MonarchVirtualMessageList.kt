package com.rookhq.monarch.components.collaboration.virtualmessagelist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.datagrid.loadmore.MonarchLoadMore
import com.rookhq.monarch.components.datagrid.loadmore.MonarchLoadMoreLabels
import com.rookhq.monarch.components.feedback.loadingstate.MonarchLoadingState
import com.rookhq.monarch.components.layout.divider.MonarchDivider
import com.rookhq.monarch.components.layout.divider.MonarchDividerVariant
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.internal.MonarchLiveStatus
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.internal.monarchTimeZone
import com.rookhq.monarch.theme.MonarchTheme
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * A conversation's messages, pinned to the bottom: day separators, a
 * "New messages" divider, older history loaded at the top, and a button
 * that jumps to the latest message.
 *
 * Built on a `LazyColumn` with `reverseLayout = true`, so only the rows on
 * screen are composed, the newest message sits at the bottom and older pages
 * added to the start of [items] do not move the view. While the reader is at
 * the bottom, new messages scroll into view (`LazyListState.requestScrollToItem`);
 * when they have scrolled up, the jump button counts the messages that
 * arrived. Arrivals are announced in a polite live region; the rows
 * themselves are not live, so scrolling stays quiet.
 *
 * The list scrolls itself: give it a bounded height (`Modifier.weight(1f)`
 * or `Modifier.fillMaxSize()`), and do not put it in a vertically scrolling
 * parent. Fetching stays in the app: it passes the loaded [items] and is
 * asked for older ones through [onLoadOlder].
 *
 * @param T the type of the messages.
 * @param items the loaded messages, oldest first.
 * @param key a stable, unique key per message, as for `LazyColumn`.
 * @param time when a message was sent, in epoch milliseconds (UTC), for day separators and grouping.
 * @param modifier the modifier of the list.
 * @param state the scroll state. Index 0 is the bottom row.
 * @param authorId the author of a message, for grouping consecutive messages. `null` never groups.
 * @param firstUnreadKey the key of the first unread message: the "New messages" divider goes above it.
 * @param hasOlder there is older history to load.
 * @param loadingOlder older history is loading.
 * @param olderFailed loading older history failed: the top row offers a retry.
 * @param loading the first load: a placeholder is shown while there are no items.
 * @param stickToBottom follows new messages while the reader is at the bottom.
 * @param jumpToLatest shows the jump button while the reader is scrolled up.
 * @param scrollToKey scrolls to this message once it is loaded, for example after opening a link to it.
 * @param loadOlderThreshold [onLoadOlder] is called when the top row is this many rows away.
 * @param groupWindowMillis consecutive messages by one author within this time are grouped.
 * @param now the current time, for Today and Yesterday. `null` reads the clock.
 * @param locale the locale of the day separators. `null` uses the configuration's.
 * @param timeZone the time zone of the day separators. An IANA id such as `"Europe/Berlin"`. `null`, or an id the
 * device does not know, is the device's zone.
 * @param labels replacements for the built-in strings.
 * @param onLoadOlder loads the previous page; prepend it to [items]. Called near the top and by the Load
 * older button.
 * @param onRetryOlder retries a failed page. `null` calls [onLoadOlder].
 * @param onAtBottomChange called when the reader reaches or leaves the bottom, for example to mark the
 * conversation read.
 * @param jumpIcon the icon of the jump button, a downward arrow `MonarchIcon` with a `null` description.
 * @param header above the first message once all history is loaded, such as the conversation's start.
 * @param footer below the last message, such as a read receipt.
 * @param status under the list, in a polite live region, such as a `MonarchTypingIndicator`.
 * @param empty shown when there are no messages and no older history. [header] by default.
 * @param error shown instead of the list when the first load failed, such as a `MonarchErrorState`.
 * @param row draws one message, usually a `MonarchMessageRow`, given its context.
 */
@Suppress("LongParameterList", "LongMethod")
@Composable
public fun <T> MonarchVirtualMessageList(
    items: List<T>,
    key: (item: T) -> Any,
    time: (item: T) -> Long,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    authorId: (item: T) -> String? = { null },
    firstUnreadKey: Any? = null,
    hasOlder: Boolean = false,
    loadingOlder: Boolean = false,
    olderFailed: Boolean = false,
    loading: Boolean = false,
    stickToBottom: Boolean = true,
    jumpToLatest: Boolean = true,
    scrollToKey: Any? = null,
    loadOlderThreshold: Int = MonarchVirtualMessageListTokens.LOAD_OLDER_THRESHOLD,
    groupWindowMillis: Long = MonarchVirtualMessageListTokens.GROUP_WINDOW_MILLIS,
    now: Long? = null,
    locale: Locale? = null,
    timeZone: String? = null,
    labels: MonarchVirtualMessageListLabels = MonarchVirtualMessageListLabels(),
    onLoadOlder: (() -> Unit)? = null,
    onRetryOlder: (() -> Unit)? = null,
    onAtBottomChange: ((Boolean) -> Unit)? = null,
    jumpIcon: (@Composable () -> Unit)? = null,
    header: (@Composable () -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
    status: (@Composable () -> Unit)? = null,
    empty: (@Composable () -> Unit)? = null,
    error: (@Composable () -> Unit)? = null,
    row: @Composable LazyItemScope.(item: T, context: MonarchVirtualMessageListContext<T>) -> Unit,
) {
    val tokens = MonarchVirtualMessageListTokens
    val zone = monarchTimeZone(timeZone)
    val showOlder = hasOlder || loadingOlder || olderFailed
    when {
        error != null -> Box(modifier) { error() }
        loading && items.isEmpty() -> MonarchLoadingState(
            label = monarchString(labels.loading, R.string.monarch_virtualmessagelist_loading),
            modifier = modifier,
            rows = tokens.PLACEHOLDER_ROWS,
        )
        items.isEmpty() && !showOlder -> Box(modifier) { (empty ?: header)?.invoke() }
        else -> {
            val keys = remember(items) { items.map(key) }
            val slotsGiven = (header != null) to (footer != null)
            val entries = remember(items, firstUnreadKey, showOlder, slotsGiven, groupWindowMillis, zone) {
                tokens.entries(
                    keys = keys,
                    times = items.map(time),
                    authors = items.map(authorId),
                    firstUnreadIndex = firstUnreadKey?.let { keys.indexOf(it) } ?: -1,
                    showOlder = showOlder,
                    hasHeader = header != null,
                    hasFooter = footer != null,
                    groupWindowMillis = groupWindowMillis,
                    timeZone = zone,
                )
            }
            VirtualMessageListBody(
                items = items,
                keys = keys,
                entries = entries,
                modifier = modifier,
                state = state,
                options = VirtualMessageListOptions(
                    hasOlder = hasOlder,
                    loadingOlder = loadingOlder,
                    olderFailed = olderFailed,
                    stickToBottom = stickToBottom,
                    jumpToLatest = jumpToLatest,
                    scrollToKey = scrollToKey,
                    loadOlderThreshold = loadOlderThreshold,
                    now = now,
                    locale = locale ?: LocalConfiguration.current.locales[0],
                    timeZone = zone,
                ),
                labels = labels,
                onLoadOlder = onLoadOlder,
                onRetryOlder = onRetryOlder,
                onAtBottomChange = onAtBottomChange,
                slots = VirtualMessageListSlots(jumpIcon, header, footer, status),
                row = row,
            )
        }
    }
}

/** The settings the body reads, grouped to keep its signature short. */
@Suppress("LongParameterList")
private class VirtualMessageListOptions(
    val hasOlder: Boolean,
    val loadingOlder: Boolean,
    val olderFailed: Boolean,
    val stickToBottom: Boolean,
    val jumpToLatest: Boolean,
    val scrollToKey: Any?,
    val loadOlderThreshold: Int,
    val now: Long?,
    val locale: Locale,
    val timeZone: TimeZone,
)

/** The app's slots the body places. */
private class VirtualMessageListSlots(
    val jumpIcon: (@Composable () -> Unit)?,
    val header: (@Composable () -> Unit)?,
    val footer: (@Composable () -> Unit)?,
    val status: (@Composable () -> Unit)?,
)

/** The newest key seen, outside of state: it is read and written in a side effect only. */
private class VirtualMessageListTracker {
    var newest: Any? = null
    var started = false
}

@Suppress("LongParameterList", "LongMethod", "CyclomaticComplexMethod")
@Composable
private fun <T> VirtualMessageListBody(
    items: List<T>,
    keys: List<Any>,
    entries: List<MonarchVirtualMessageListTokens.Entry>,
    modifier: Modifier,
    state: LazyListState,
    options: VirtualMessageListOptions,
    labels: MonarchVirtualMessageListLabels,
    onLoadOlder: (() -> Unit)?,
    onRetryOlder: (() -> Unit)?,
    onAtBottomChange: ((Boolean) -> Unit)?,
    slots: VirtualMessageListSlots,
    row: @Composable LazyItemScope.(item: T, context: MonarchVirtualMessageListContext<T>) -> Unit,
) {
    val tokens = MonarchVirtualMessageListTokens
    val colors = MonarchTheme.colors
    val scope = rememberCoroutineScope()
    val reducedMotion = MonarchTheme.motion.reducedMotion
    val thresholdPx = with(LocalDensity.current) { tokens.AtBottomThreshold.roundToPx() }
    val atBottom by remember(state, thresholdPx) {
        derivedStateOf {
            tokens.atBottom(state.firstVisibleItemIndex, state.firstVisibleItemScrollOffset, thresholdPx)
        }
    }
    var unseen by remember { mutableIntStateOf(0) }
    var arrived by remember { mutableIntStateOf(0) }
    val tracker = remember { VirtualMessageListTracker() }
    val newest = keys.lastOrNull()
    SideEffect {
        // Runs after composition and before the list measures, so the scroll request applies to this frame.
        if (newest != tracker.newest) {
            val count = if (tracker.started) tokens.arrived(tracker.newest, keys) else 0
            if (count > 0) {
                val bottom = tokens.atBottom(
                    state.firstVisibleItemIndex,
                    state.firstVisibleItemScrollOffset,
                    thresholdPx,
                )
                if (options.stickToBottom && bottom) state.requestScrollToItem(0) else unseen += count
                arrived = count
            }
            tracker.newest = newest
            tracker.started = true
        }
    }

    val currentOnAtBottomChange by rememberUpdatedState(onAtBottomChange)
    LaunchedEffect(state) {
        snapshotFlow { atBottom }.distinctUntilChanged().collect { bottom ->
            if (bottom) unseen = 0
            currentOnAtBottomChange?.invoke(bottom)
        }
    }

    val currentOnLoadOlder by rememberUpdatedState(onLoadOlder)
    LaunchedEffect(state, options.hasOlder, options.loadingOlder, options.olderFailed, entries.size) {
        snapshotFlow { state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .map { last ->
                tokens.shouldLoadOlder(
                    lastVisibleIndex = last,
                    rowCount = entries.size,
                    threshold = options.loadOlderThreshold,
                    hasOlder = options.hasOlder,
                    loadingOlder = options.loadingOlder,
                    olderFailed = options.olderFailed,
                )
            }
            .distinctUntilChanged()
            .filter { it }
            .collect { currentOnLoadOlder?.invoke() }
    }

    val target = options.scrollToKey?.let { wanted ->
        entries.indexOfFirst { it is MonarchVirtualMessageListTokens.Entry.Message && it.key == wanted }
    } ?: -1
    LaunchedEffect(options.scrollToKey, target >= 0) {
        if (target >= 0) state.scrollToItem(target)
    }

    val listName = monarchString(labels.label, R.string.monarch_virtualmessagelist_label)
    val today = monarchString(labels.today, R.string.monarch_virtualmessagelist_today)
    val yesterday = monarchString(labels.yesterday, R.string.monarch_virtualmessagelist_yesterday)
    val newMessages = monarchString(labels.newMessages, R.string.monarch_virtualmessagelist_new_messages)
    val olderLabels = MonarchLoadMoreLabels(
        loadMore = monarchString(labels.loadOlder, R.string.monarch_virtualmessagelist_load_older),
        loading = monarchString(labels.loadingOlder, R.string.monarch_virtualmessagelist_loading_older),
        failed = monarchString(labels.olderFailed, R.string.monarch_virtualmessagelist_older_failed),
        retry = monarchString(labels.retryOlder, R.string.monarch_virtualmessagelist_retry_older),
    )
    val clock = options.now ?: System.currentTimeMillis()

    Column(modifier) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .semantics { contentDescription = listName },
                state = state,
                reverseLayout = true,
            ) {
                items(entries, key = { it.key }, contentType = { it::class }) { entry ->
                    when (entry) {
                        is MonarchVirtualMessageListTokens.Entry.Message -> {
                            val item = items[entry.index]
                            row(
                                item,
                                MonarchVirtualMessageListContext(
                                    grouped = entry.grouped,
                                    index = entry.index,
                                    previous = items.getOrNull(entry.index - 1),
                                ),
                            )
                        }
                        is MonarchVirtualMessageListTokens.Entry.Day -> VirtualMessageListSeparator(
                            text = tokens.dayLabel(
                                entry.dayStart,
                                clock,
                                options.locale,
                                options.timeZone,
                                today,
                                yesterday,
                            ),
                            color = tokens.dayColor(colors),
                            lineColor = null,
                        )
                        MonarchVirtualMessageListTokens.Entry.Unread -> VirtualMessageListSeparator(
                            text = newMessages,
                            color = tokens.unreadColor(colors),
                            lineColor = tokens.unreadLineColor(colors),
                        )
                        MonarchVirtualMessageListTokens.Entry.Older -> MonarchLoadMore(
                            hasMore = options.hasOlder || options.olderFailed,
                            onLoadMore = { onLoadOlder?.invoke() },
                            loading = options.loadingOlder,
                            failed = options.olderFailed,
                            showEnd = false,
                            onRetry = onRetryOlder ?: onLoadOlder,
                            labels = olderLabels,
                        )
                        MonarchVirtualMessageListTokens.Entry.Header -> slots.header?.invoke()
                        MonarchVirtualMessageListTokens.Entry.Footer -> slots.footer?.invoke()
                    }
                }
            }
            if (options.jumpToLatest && !atBottom) {
                val text = if (unseen > 0) {
                    labels.unseen?.invoke(unseen)
                        ?: pluralStringResource(R.plurals.monarch_virtualmessagelist_unseen, unseen, unseen)
                } else {
                    monarchString(labels.jumpToLatest, R.string.monarch_virtualmessagelist_jump_to_latest)
                }
                MonarchButton(
                    text = text,
                    onClick = {
                        scope.launch { if (reducedMotion) state.scrollToItem(0) else state.animateScrollToItem(0) }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(tokens.JumpPadding),
                    size = MonarchButtonSize.Sm,
                    leadingIcon = slots.jumpIcon,
                )
            }
        }
        // Always present, so a change is announced.
        Box(Modifier.fillMaxWidth().semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }) {
            slots.status?.invoke()
        }
        val arrivedText = if (arrived > 0) {
            labels.arrived?.invoke(arrived)
                ?: pluralStringResource(R.plurals.monarch_virtualmessagelist_arrived, arrived, arrived)
        } else {
            ""
        }
        MonarchLiveStatus(text = arrivedText.takeIf { it.isNotEmpty() }, visible = false)
    }
}

/** A day separator or the "New messages" divider: a caption between two lines, a TalkBack heading. */
@Composable
private fun VirtualMessageListSeparator(text: String, color: Color, lineColor: Color?) {
    val tokens = MonarchVirtualMessageListTokens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = tokens.HorizontalPadding, vertical = tokens.SeparatorPadding)
            .semantics(mergeDescendants = true) { heading() },
        horizontalArrangement = Arrangement.spacedBy(tokens.SeparatorGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SeparatorLine(lineColor)
        MonarchText(text = text, style = MonarchTextStyle.Caption, color = color)
        SeparatorLine(lineColor)
    }
}

/**
 * One hairline of a separator: the subtle `MonarchDivider` for a day ([color] `null`), or a drawn line in the
 * accent border colour for "New messages", a colour `MonarchDivider` does not offer.
 */
@Composable
private fun RowScope.SeparatorLine(color: Color?) {
    if (color == null) {
        MonarchDivider(modifier = Modifier.weight(1f), variant = MonarchDividerVariant.Subtle)
    } else {
        Box(Modifier.weight(1f).height(MonarchShapeDefaults.BorderWidth).background(color))
    }
}
