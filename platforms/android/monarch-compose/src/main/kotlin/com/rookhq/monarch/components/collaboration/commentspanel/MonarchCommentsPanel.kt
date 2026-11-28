package com.rookhq.monarch.components.collaboration.commentspanel

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.actions.iconbutton.MonarchIconButton
import com.rookhq.monarch.components.collaboration.commentthread.MonarchCommentThread
import com.rookhq.monarch.components.feedback.emptystate.MonarchEmptyState
import com.rookhq.monarch.components.feedback.emptystate.MonarchEmptyStateSize
import com.rookhq.monarch.components.feedback.inlineerrorretry.MonarchInlineErrorRetry
import com.rookhq.monarch.components.feedback.loadingstate.MonarchLoadingState
import com.rookhq.monarch.components.forms.segmentedcontrol.MonarchSegmentedControl
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.internal.MonarchDefaultIcons
import com.rookhq.monarch.internal.MonarchLiveStatus
import com.rookhq.monarch.internal.rememberMonarchRelativeClock
import com.rookhq.monarch.theme.MonarchTheme
import java.util.Locale

/**
 * Every comment thread of a document or a record, in one panel: Open and Resolved threads in a list, and one
 * thread at a time with its quote, its replies, Resolve or Reopen, and deleting behind a confirmation.
 *
 * Built by Monarch as the phone's list and detail pattern, for a Material 3 `ModalBottomSheet` or a supporting
 * pane beside the document at expanded width: a heading with an optional Close button, a
 * `MonarchSegmentedControl` for Open and Resolved, a `LazyColumn` of `MonarchListRow` rows (the
 * starter's avatar and name, the quote, the number of comments and the last activity), and, for the selected
 * thread, a `MonarchCommentThread` under an All comments button. The system back gesture also returns to the
 * list. Loading shows a `MonarchLoadingState`, an error a `MonarchInlineErrorRetry`, and an empty filter a
 * `MonarchEmptyState`.
 *
 * The panel never fetches and knows nothing about the editor: the app passes [threads] and saves through the
 * suspending callbacks, which show progress while they run and report a thrown error in the thread. Selecting
 * a thread calls [onSelectedThreadChange], where the app can highlight the quoted passage. After a thread is
 * resolved or reopened the panel returns to the list and announces it through a polite live region that is
 * always present. Give the panel a bounded height, as for any lazy list.
 *
 * @param threads the threads, or `null` while they load. They are listed under Open or Resolved by
 * [MonarchCommentsPanelThread.resolved].
 * @param filter which threads are listed.
 * @param onFilterChange called when the person switches between Open and Resolved; the open thread is closed.
 * @param selectedThreadId the id of the thread shown in full, or `null` for the list.
 * @param onSelectedThreadChange called with a thread opened from the list, or `null` when it is closed.
 * @param modifier the modifier of the panel.
 * @param errorMessage loading the threads failed: shown instead of the list.
 * @param now the current time in epoch milliseconds, for the relative times. `null` follows the clock, refreshed
 * every minute.
 * @param locale the locale of the relative times in the list. `null` uses the first locale of the configuration.
 * @param labels replacements for the built-in strings, including the thread's.
 * @param onRetry shows Try again with [errorMessage].
 * @param onClose shows a Close button in the header.
 * @param onReply posts a reply to a thread; shows the reply field on open threads with `canReply`. Throw to keep
 * the text and report a failure.
 * @param onResolvedChange resolves (`true`) or reopens (`false`) a thread; shows Resolve or Reopen on threads
 * with `canResolve`. Throw to report a failure; on success the panel returns to the list.
 * @param onDeleteComment deletes a comment after confirmation; offered on comments with `canDelete`. Throw to
 * report a failure.
 * @param emptyIcon the icon of the empty state, a `MonarchIcon` with a `null` description.
 * @param backIcon the icon before All comments, a back arrow `MonarchIcon` with a `null` description that mirrors
 * in right to left.
 */
@Suppress("LongParameterList", "LongMethod", "CyclomaticComplexMethod")
@ExperimentalMonarchApi
@Composable
public fun MonarchCommentsPanel(
    threads: List<MonarchCommentsPanelThread>?,
    filter: MonarchCommentsPanelFilter,
    onFilterChange: (MonarchCommentsPanelFilter) -> Unit,
    selectedThreadId: String?,
    onSelectedThreadChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    now: Long? = null,
    locale: Locale? = null,
    labels: MonarchCommentsPanelLabels = MonarchCommentsPanelLabels(),
    onRetry: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    onReply: (suspend (threadId: String, body: String) -> Unit)? = null,
    onResolvedChange: (suspend (threadId: String, resolved: Boolean) -> Unit)? = null,
    onDeleteComment: (suspend (threadId: String, commentId: String) -> Unit)? = null,
    emptyIcon: (@Composable () -> Unit)? = null,
    backIcon: (@Composable () -> Unit)? = null,
) {
    val tokens = MonarchCommentsPanelTokens
    val strings = monarchCommentsPanelStrings(labels)
    val context = MonarchCommentsPanelContext(
        now = rememberMonarchRelativeClock(
            threads.orEmpty().map { MonarchCommentsPanelTokens.lastActivity(it) },
            fixed = now,
        ),
        locale = locale ?: LocalConfiguration.current.locales[0],
        labels = labels,
        strings = strings,
    )
    var notice by remember { mutableStateOf("") }
    var focusTarget by remember { mutableStateOf<String?>(null) }
    val visible = tokens.visible(threads.orEmpty(), filter)
    val thread = if (threads == null || errorMessage != null) null else tokens.selected(selectedThreadId, visible)
    val shown by rememberUpdatedState(thread?.id)

    val open: (String) -> Unit = { id ->
        notice = ""
        focusTarget = tokens.BACK_TARGET
        onSelectedThreadChange(id)
    }
    val back: () -> Unit = {
        focusTarget = shown
        onSelectedThreadChange(null)
    }

    val resolve: (suspend (String, Boolean) -> Unit)? = if (onResolvedChange == null) {
        null
    } else {
        { id, value ->
            onResolvedChange(id, value)
            notice = if (value) strings.resolvedNotice else strings.reopenedNotice
            // Still on screen: back to the list, with focus on the row the reader came from or the first one.
            if (shown == id) {
                focusTarget = id
                onSelectedThreadChange(null)
            }
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(tokens.Gap)) {
        CommentsPanelHeader(strings, onClose)
        MonarchSegmentedControl(modifier = Modifier.padding(horizontal = tokens.Inset), fillWidth = true) {
            MonarchCommentsPanelFilter.entries.forEach { option ->
                Segment(
                    selected = filter == option,
                    onClick = {
                        if (filter != option) {
                            onFilterChange(option)
                            if (selectedThreadId != null) onSelectedThreadChange(null)
                        }
                    },
                    label = if (option == MonarchCommentsPanelFilter.Open) strings.open else strings.resolved,
                )
            }
        }
        Box(modifier = Modifier.weight(1f, fill = false).fillMaxWidth()) {
            when {
                errorMessage != null -> CommentsPanelError(errorMessage, onRetry)
                threads == null -> MonarchLoadingState(
                    label = strings.loading,
                    modifier = Modifier.padding(horizontal = tokens.Inset),
                    rows = tokens.PLACEHOLDER_ROWS,
                )
                thread != null -> {
                    // The system back gesture returns to the list, as in a navigation stack.
                    BackHandler(onBack = back)
                    CommentsPanelDetail(
                        thread = thread,
                        context = context,
                        focusBack = focusTarget == tokens.BACK_TARGET,
                        onFocused = { focusTarget = null },
                        onBack = back,
                        onReply = onReply,
                        onResolvedChange = resolve,
                        onDeleteComment = onDeleteComment,
                        backIcon = backIcon,
                    )
                }
                visible.isEmpty() -> MonarchEmptyState(
                    title = if (filter == MonarchCommentsPanelFilter.Open) strings.emptyOpen else strings.emptyResolved,
                    modifier = Modifier.padding(horizontal = tokens.Inset),
                    size = MonarchEmptyStateSize.Sm,
                    message = if (filter == MonarchCommentsPanelFilter.Open) strings.emptyHint else null,
                    icon = emptyIcon,
                )
                else -> MonarchCommentsPanelList(
                    threads = visible,
                    context = context,
                    focusRow = tokens.focusRow(focusTarget, visible),
                    onFocused = { focusTarget = null },
                    onOpen = open,
                )
            }
        }
        // Always present, so resolving and reopening are announced as a change of this region.
        MonarchLiveStatus(text = notice.takeIf { it.isNotEmpty() }, visible = false)
    }
}

/** What the list and the thread share. */
internal class MonarchCommentsPanelContext(
    val now: Long,
    val locale: Locale,
    val labels: MonarchCommentsPanelLabels,
    val strings: MonarchCommentsPanelStrings,
)

/** The heading and Close. */
@Composable
private fun CommentsPanelHeader(strings: MonarchCommentsPanelStrings, onClose: (() -> Unit)?) {
    val tokens = MonarchCommentsPanelTokens
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = tokens.Inset, end = tokens.Gap),
        horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = strings.title,
            modifier = Modifier.weight(1f).semantics { heading() },
            style = MonarchTheme.typography.subheading,
            color = tokens.titleColor(MonarchTheme.colors),
        )
        if (onClose != null) {
            // The web closes the panel with a close mark, named "Close".
            MonarchIconButton(label = strings.close, onClick = onClose) {
                Box(Modifier.clearAndSetSemantics { }) { MonarchDefaultIcons.Close() }
            }
        }
    }
}

/** A loading error, with Try again when the app can retry. */
@Composable
private fun CommentsPanelError(message: String, onRetry: (() -> Unit)?) {
    val tokens = MonarchCommentsPanelTokens
    if (onRetry != null) {
        MonarchInlineErrorRetry(
            message = message,
            onRetry = onRetry,
            modifier = Modifier.padding(horizontal = tokens.Inset),
        )
    } else {
        MonarchText(
            text = message,
            modifier = Modifier
                .padding(horizontal = tokens.Inset)
                .semantics { liveRegion = LiveRegionMode.Polite },
            color = tokens.errorColor(MonarchTheme.colors),
        )
    }
}

/** One thread in full under All comments; its reply field starts empty for each thread. */
@OptIn(ExperimentalMonarchApi::class)
@Suppress("LongParameterList")
@Composable
private fun CommentsPanelDetail(
    thread: MonarchCommentsPanelThread,
    context: MonarchCommentsPanelContext,
    focusBack: Boolean,
    onFocused: () -> Unit,
    onBack: () -> Unit,
    onReply: (suspend (String, String) -> Unit)?,
    onResolvedChange: (suspend (String, Boolean) -> Unit)?,
    onDeleteComment: (suspend (String, String) -> Unit)?,
    backIcon: (@Composable () -> Unit)?,
) {
    val tokens = MonarchCommentsPanelTokens
    val requester = remember { FocusRequester() }
    LaunchedEffect(focusBack, thread.id) {
        if (focusBack) {
            withFrameNanos { }
            requester.requestFocus()
            onFocused()
        }
    }
    val resolve: (suspend (Boolean) -> Unit)? = if (onResolvedChange == null || !thread.canResolve) {
        null
    } else {
        { value -> onResolvedChange(thread.id, value) }
    }
    val reply: (suspend (String) -> Unit)? = if (onReply == null || !thread.canReply) {
        null
    } else {
        { body -> onReply(thread.id, body) }
    }
    val delete: (suspend (String) -> Unit)? = if (onDeleteComment == null) {
        null
    } else {
        { id -> onDeleteComment(thread.id, id) }
    }
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(tokens.SmallGap),
    ) {
        MonarchButton(
            text = context.strings.back,
            onClick = onBack,
            modifier = Modifier.padding(horizontal = tokens.Gap).focusRequester(requester),
            variant = MonarchButtonVariant.Ghost,
            size = MonarchButtonSize.Sm,
            leadingIcon = backIcon,
        )
        key(thread.id) {
            MonarchCommentThread(
                comments = thread.comments,
                quote = thread.quote,
                resolved = thread.resolved,
                now = context.now,
                labels = context.labels.thread,
                onResolvedChange = resolve,
                onReply = reply,
                onDeleteComment = delete,
            )
        }
    }
}
