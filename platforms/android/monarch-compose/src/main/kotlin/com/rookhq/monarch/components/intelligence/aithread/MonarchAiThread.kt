package com.rookhq.monarch.components.intelligence.aithread

import android.icu.util.TimeZone
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.datagrid.loadmore.MonarchLoadMore
import com.rookhq.monarch.components.datagrid.loadmore.MonarchLoadMoreLabels
import com.rookhq.monarch.components.feedback.inlinenotice.MonarchInlineNotice
import com.rookhq.monarch.components.feedback.inlinenotice.MonarchInlineNoticeVariant
import com.rookhq.monarch.components.intelligence.aicontextdivider.MonarchAiContextDivider
import com.rookhq.monarch.components.intelligence.aicontextdivider.MonarchAiContextDividerKind
import com.rookhq.monarch.components.intelligence.aicontextdivider.MonarchAiContextDividerLabels
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiConversationError
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiConversationMessage
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiConversationStatus
import com.rookhq.monarch.components.intelligence.aimessage.MonarchAiMessage
import com.rookhq.monarch.components.intelligence.aithinkingindicator.MonarchAiThinkingIndicator
import com.rookhq.monarch.internal.MonarchDefaultIcons
import com.rookhq.monarch.internal.MonarchLiveStatus
import com.rookhq.monarch.internal.currentIcuTimeZone
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme
import kotlinx.coroutines.launch
import java.util.Locale

private typealias Tokens = MonarchAiThreadTokens

/**
 * The scrolling list of a conversation: its messages oldest first with day
 * labels, pinned to the newest while the person is at the bottom, a "Jump to
 * latest" button that counts new messages when they are not, "Load earlier
 * messages" at the top, a thinking indicator while a reply is submitted, a
 * notice while offline or slow, and one announcer for the whole conversation.
 *
 * Built on a `LazyColumn` with `reverseLayout = true`, so only the messages on
 * screen are composed and older messages added to the start of [messages] do
 * not move the view. Each message is a `MonarchAiMessage` unless [message]
 * draws it; day labels are `MonarchAiContextDivider`s, the indicator is
 * `MonarchAiThinkingIndicator`, the top row is `MonarchLoadMore` and the
 * notice is a `MonarchInlineNotice`. The composer is not part of the thread:
 * put a `MonarchAiPromptInput` under it.
 *
 * The thread scrolls itself: give it a bounded height (`Modifier.weight(1f)`
 * or `Modifier.fillMaxSize()`), and do not put it in a vertically scrolling
 * parent.
 *
 * With [announces], TalkBack hears "Thinking…" when a reply is submitted, the
 * reply in whole sentences at most once per [announceIntervalMillis], then
 * "Response complete" or "Response stopped", from one polite live region; the
 * messages are told not to announce themselves. A conversation error that no
 * message shows is announced assertively.
 *
 * @param messages the visible conversation, oldest first (`MonarchAiConversation.messages`).
 * @param modifier the modifier of the thread.
 * @param state the scroll state. Index 0 is the bottom row.
 * @param contentPadding the padding around the messages, inside the scrolling area.
 * @param status `MonarchAiConversation.status`. `Submitted` shows the thinking indicator; `Submitted` and
 * `Streaming` make the Escape key call [onStop].
 * @param error `MonarchAiConversation.error`, announced when the newest message has no error part.
 * @param offline `!MonarchAiConversation.online`: shows and announces a notice under the messages.
 * @param queuedCount how many messages wait for the connection (`MonarchAiConversation.queued.size`), named in
 * the notice.
 * @param slow `MonarchAiConversation.slow`: shows "taking longer than usual" while a reply is running.
 * @param hasEarlier whether older messages exist. With [onLoadEarlier], shows "Load earlier messages" at the top.
 * @param loadingEarlier shows the load button as busy.
 * @param daySeparators shows a day label above the first message of each calendar day.
 * @param timeZone the IANA zone of the day labels and of the default messages' times. `null` is the device's.
 * @param nowMillis the reference time for "Today" and "Yesterday", in epoch milliseconds (UTC). `null` reads
 * the clock.
 * @param announces whether the thread announces for the conversation. Pass `false` to let each message announce
 * itself instead.
 * @param announceIntervalMillis the least time between two polite announcements of a streaming reply.
 * @param labels replacements for the built-in strings.
 * @param onStop called by the Escape key inside the thread while a reply is running.
 * @param onLoadEarlier loads older messages; prepend them to [messages].
 * @param jumpIcon the icon of the jump button, for example a downward arrow `MonarchIcon` with a `null`
 * description. Monarch bundles no icons.
 * @param empty shown when there are no messages, no older ones and no reply is submitted, for example a
 * `MonarchAiWelcome` with suggested prompts.
 * @param message draws one message, given where it sits. Pass [MonarchAiThreadMessageContext.announces] on to
 * `MonarchAiMessage`. The default is a `MonarchAiMessage` with the thread's [timeZone].
 */
@ExperimentalMonarchApi
@Suppress("LongParameterList", "LongMethod")
@Composable
public fun MonarchAiThread(
    messages: List<MonarchAiConversationMessage>,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(),
    status: MonarchAiConversationStatus = MonarchAiConversationStatus.Idle,
    error: MonarchAiConversationError? = null,
    offline: Boolean = false,
    queuedCount: Int = 0,
    slow: Boolean = false,
    hasEarlier: Boolean = false,
    loadingEarlier: Boolean = false,
    daySeparators: Boolean = true,
    timeZone: String? = null,
    nowMillis: Long? = null,
    announces: Boolean = true,
    announceIntervalMillis: Long = 1_000L,
    labels: MonarchAiThreadLabels = MonarchAiThreadLabels(),
    onStop: (() -> Unit)? = null,
    onLoadEarlier: (() -> Unit)? = null,
    jumpIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.ArrowDown,
    empty: (@Composable () -> Unit)? = null,
    message: @Composable LazyItemScope.(
        message: MonarchAiConversationMessage,
        context: MonarchAiThreadMessageContext,
    ) -> Unit = { item, context ->
        MonarchAiMessage(message = item, announces = context.announces, timeZone = timeZone)
    },
) {
    val zone = remember(timeZone) { timeZone?.let { TimeZone.getTimeZone(it) } ?: currentIcuTimeZone() }
    val locale = LocalConfiguration.current.locales[0]
    val busy = Tokens.busy(status)
    val thinking = status == MonarchAiConversationStatus.Submitted
    val showEarlier = hasEarlier && onLoadEarlier != null
    val ids = remember(messages) { messages.map { it.id } }
    val times = remember(messages) { messages.map { it.createdAtMillis } }
    val entries = remember(ids, times, daySeparators, thinking, showEarlier, zone) {
        Tokens.entries(ids, times, daySeparators, thinking, showEarlier, zone)
    }
    val notice = Tokens.notice(offline, queuedCount, slow, busy)
    val thinkingText = monarchString(labels.thinking, R.string.monarch_aithread_thinking)
    val showsEmpty = Tokens.showsEmpty(messages.size, thinking, showEarlier, empty != null)

    Column(modifier = modifier.aiThreadStopOnEscape(busy, onStop)) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (showsEmpty && empty != null) {
                Box(Modifier.fillMaxSize()) { empty() }
            } else {
                AiThreadList(
                    messages = messages,
                    entries = entries,
                    state = state,
                    contentPadding = contentPadding,
                    options = AiThreadListOptions(
                        loadingEarlier = loadingEarlier,
                        nowMillis = nowMillis,
                        locale = locale,
                        timeZone = zone,
                        announces = announces,
                        thinkingText = thinkingText,
                    ),
                    labels = labels,
                    onLoadEarlier = onLoadEarlier,
                    jumpIcon = jumpIcon,
                    message = message,
                )
            }
        }
        AiThreadNotice(notice, queuedCount, labels)
        if (announces) AiThreadAnnouncements(messages.lastOrNull(), status, error, announceIntervalMillis, labels)
    }
}

/** Calls [onStop] when Escape is pressed inside the thread while a reply is running. */
@Composable
private fun Modifier.aiThreadStopOnEscape(busy: Boolean, onStop: (() -> Unit)?): Modifier {
    val currentOnStop by rememberUpdatedState(onStop)
    val currentBusy by rememberUpdatedState(busy)
    return onPreviewKeyEvent { event ->
        val stop = currentOnStop.takeIf { currentBusy }
        val escape = event.type == KeyEventType.KeyDown && event.key == Key.Escape
        if (escape && stop != null) stop()
        escape && stop != null
    }
}

/**
 * The notice under the list, and an always-present polite live region outside the list that holds its text, so
 * it is announced once as a status when it appears or changes.
 */
@Composable
private fun AiThreadNotice(notice: AiThreadNoticeKind?, queuedCount: Int, labels: MonarchAiThreadLabels) {
    val text = notice?.let { noticeText(it, queuedCount, labels) }.orEmpty()
    Column(Modifier.fillMaxWidth()) {
        notice?.let { kind ->
            MonarchInlineNotice(
                message = text,
                modifier = Modifier.padding(top = Tokens.NoticePadding),
                tone = Tokens.noticeTone(kind),
                variant = MonarchInlineNoticeVariant.Plain,
            )
        }
        MonarchLiveStatus(text = text.takeIf { it.isNotEmpty() }, visible = false)
    }
}

/** The thread's announcer with its strings resolved. */
@Composable
private fun AiThreadAnnouncements(
    last: MonarchAiConversationMessage?,
    status: MonarchAiConversationStatus,
    error: MonarchAiConversationError?,
    intervalMillis: Long,
    labels: MonarchAiThreadLabels,
) {
    val errorText = error?.takeIf { Tokens.announcesError(true, last) }?.let {
        labels.error?.invoke(it.message) ?: stringResource(R.string.monarch_aithread_error, it.message)
    }
    AiThreadAnnouncer(
        last = last,
        status = status,
        intervalMillis = intervalMillis,
        texts = AiThreadAnnouncerTexts(
            thinking = monarchString(labels.thinking, R.string.monarch_aithread_thinking),
            responseComplete = monarchString(labels.responseComplete, R.string.monarch_aithread_response_complete),
            responseStopped = monarchString(labels.responseStopped, R.string.monarch_aithread_response_stopped),
            codeBlock = monarchString(labels.codeBlock, R.string.monarch_aithread_code_block),
        ),
        errorText = errorText,
    )
}

@Composable
private fun noticeText(kind: AiThreadNoticeKind, queuedCount: Int, labels: MonarchAiThreadLabels): String =
    when (kind) {
        AiThreadNoticeKind.Offline -> if (queuedCount > 0) {
            labels.offlineQueued?.invoke(queuedCount)
                ?: pluralStringResource(R.plurals.monarch_aithread_offline_queued, queuedCount, queuedCount)
        } else {
            monarchString(labels.offline, R.string.monarch_aithread_offline)
        }
        AiThreadNoticeKind.Sending -> labels.sendingQueued?.invoke(queuedCount)
            ?: pluralStringResource(R.plurals.monarch_aithread_sending_queued, queuedCount, queuedCount)
        AiThreadNoticeKind.Slow -> monarchString(labels.slow, R.string.monarch_aithread_slow)
    }

/** The settings the list reads, grouped to keep its signature short. */
private class AiThreadListOptions(
    val loadingEarlier: Boolean,
    val nowMillis: Long?,
    val locale: Locale,
    val timeZone: TimeZone,
    val announces: Boolean,
    val thinkingText: String,
)

/** The newest row key seen, outside of state: it is read and written in a side effect only. */
private class AiThreadTracker {
    var newest: Any? = null
    var started = false
}

@Suppress("LongParameterList", "LongMethod")
@Composable
private fun AiThreadList(
    messages: List<MonarchAiConversationMessage>,
    entries: List<MonarchAiThreadTokens.Entry>,
    state: LazyListState,
    contentPadding: PaddingValues,
    options: AiThreadListOptions,
    labels: MonarchAiThreadLabels,
    onLoadEarlier: (() -> Unit)?,
    jumpIcon: (@Composable () -> Unit)?,
    message: @Composable LazyItemScope.(MonarchAiConversationMessage, MonarchAiThreadMessageContext) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val reducedMotion = MonarchTheme.motion.reducedMotion
    val thresholdPx = with(LocalDensity.current) { Tokens.AtBottomThreshold.roundToPx() }
    val atBottom by remember(state, thresholdPx) {
        derivedStateOf { Tokens.atBottom(state.firstVisibleItemIndex, state.firstVisibleItemScrollOffset, thresholdPx) }
    }
    val tracker = remember { AiThreadTracker() }
    val newest = entries.firstOrNull()?.key
    SideEffect {
        // Runs after composition and before the list measures, so the scroll request applies to this frame.
        if (newest != tracker.newest) {
            val bottom = Tokens.atBottom(state.firstVisibleItemIndex, state.firstVisibleItemScrollOffset, thresholdPx)
            if (tracker.started && bottom) state.requestScrollToItem(0)
            tracker.newest = newest
            tracker.started = true
        }
    }
    val newestId = messages.lastOrNull()?.id
    var lastSeen by remember { mutableStateOf(newestId) }
    LaunchedEffect(atBottom, newestId) { if (atBottom) lastSeen = newestId }
    val unseen = if (atBottom) 0 else Tokens.newCount(messages.map { it.id }, lastSeen)

    val listName = monarchString(labels.label, R.string.monarch_aithread_label)
    val texts = AiThreadRowTexts(
        today = monarchString(labels.today, R.string.monarch_aithread_today),
        yesterday = monarchString(labels.yesterday, R.string.monarch_aithread_yesterday),
        earlier = MonarchLoadMoreLabels(
            loadMore = monarchString(labels.loadEarlier, R.string.monarch_aithread_load_earlier),
            loading = monarchString(labels.loadingEarlier, R.string.monarch_aithread_loading_earlier),
        ),
        clock = options.nowMillis ?: System.currentTimeMillis(),
    )

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .semantics { contentDescription = listName },
            state = state,
            contentPadding = contentPadding,
            reverseLayout = true,
            verticalArrangement = Arrangement.spacedBy(Tokens.MessageGap, Alignment.Bottom),
        ) {
            items(entries, key = { it.key }, contentType = { it::class }) { entry ->
                AiThreadRow(entry, messages, options, texts, onLoadEarlier, message)
            }
        }
        if (!atBottom && messages.isNotEmpty()) {
            AiThreadJumpButton(
                unseen = unseen,
                labels = labels,
                jumpIcon = jumpIcon,
                onClick = {
                    scope.launch { if (reducedMotion) state.scrollToItem(0) else state.animateScrollToItem(0) }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(Tokens.JumpPadding),
            )
        }
    }
}

/** The resolved strings and clock of the rows. */
private class AiThreadRowTexts(
    val today: String,
    val yesterday: String,
    val earlier: MonarchLoadMoreLabels,
    val clock: Long,
)

/** One row of the list: a message, a day label, the thinking indicator or the load button. */
@Suppress("LongParameterList")
@Composable
private fun LazyItemScope.AiThreadRow(
    entry: MonarchAiThreadTokens.Entry,
    messages: List<MonarchAiConversationMessage>,
    options: AiThreadListOptions,
    texts: AiThreadRowTexts,
    onLoadEarlier: (() -> Unit)?,
    message: @Composable LazyItemScope.(MonarchAiConversationMessage, MonarchAiThreadMessageContext) -> Unit,
) {
    when (entry) {
        is MonarchAiThreadTokens.Entry.Message -> message(
            messages[entry.index],
            MonarchAiThreadMessageContext(
                index = entry.index,
                isLast = entry.index == messages.lastIndex,
                announces = !options.announces,
            ),
        )
        is MonarchAiThreadTokens.Entry.Day -> MonarchAiContextDivider(
            kind = MonarchAiContextDividerKind.Day,
            labels = MonarchAiContextDividerLabels(
                label = Tokens.dayLabel(
                    entry.dayStart,
                    texts.clock,
                    options.locale,
                    options.timeZone,
                    texts.today,
                    texts.yesterday,
                ),
            ),
        )
        MonarchAiThreadTokens.Entry.Thinking -> MonarchAiThinkingIndicator(
            label = options.thinkingText,
            announce = !options.announces,
        )
        MonarchAiThreadTokens.Entry.Earlier -> MonarchLoadMore(
            hasMore = true,
            onLoadMore = { onLoadEarlier?.invoke() },
            loading = options.loadingEarlier,
            showEnd = false,
            labels = texts.earlier,
        )
    }
}

/** "Jump to latest", with the count of messages that arrived below. */
@Composable
private fun AiThreadJumpButton(
    unseen: Int,
    labels: MonarchAiThreadLabels,
    jumpIcon: (@Composable () -> Unit)?,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    MonarchButton(onClick = onClick, modifier = modifier, size = MonarchButtonSize.Sm) {
        jumpIcon?.let {
            it()
            Spacer(Modifier.width(Tokens.JumpGap))
        }
        Text(monarchString(labels.jumpToLatest, R.string.monarch_aithread_jump_to_latest))
        if (unseen > 0) {
            Spacer(Modifier.width(Tokens.JumpGap))
            Text(
                labels.newMessages?.invoke(unseen)
                    ?: pluralStringResource(R.plurals.monarch_aithread_new_messages, unseen, unseen),
            )
        }
    }
}
