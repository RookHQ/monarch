package com.rookhq.monarch.components.collaboration.commentthread

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.collaboration.messagecomposer.MonarchMessageComposer
import com.rookhq.monarch.components.collaboration.messagecomposer.MonarchMessageComposerLabels
import com.rookhq.monarch.components.actions.iconbutton.MonarchIconButton
import com.rookhq.monarch.components.collaboration.messagerow.MonarchMessageRow
import com.rookhq.monarch.components.collaboration.messagerow.MonarchMessageRowAction
import com.rookhq.monarch.components.collaboration.messagerow.MonarchMessageRowAuthor
import com.rookhq.monarch.components.collaboration.messagerow.MonarchMessageRowLabels
import com.rookhq.monarch.components.datadisplay.badge.MonarchBadge
import com.rookhq.monarch.components.overlays.dialog.MonarchDialog
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.MonarchDefaultIcons
import com.rookhq.monarch.internal.monarchGhostStartAlign
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * One threaded discussion shown in place, next to the thing it is about:
 * an optional quote, the starting comment, its replies with older ones
 * folded, Resolve and Reopen, and a reply field.
 *
 * A Monarch pattern built from `MonarchMessageRow` for each comment,
 * `MonarchMessageComposer` for the reply field, `MonarchButton`,
 * `MonarchBadge` and a `MonarchDialog` that confirms deleting. It
 * is a `Column`, not a lazy list, so it can sit in a scrolling screen or a
 * sheet: the starting comment and the latest [visibleReplies] replies stay
 * visible and the rest fold behind "Show N earlier replies".
 *
 * The app saves. [onReply], [onResolvedChange] and [onDeleteComment] are
 * suspending functions: the control shows progress while they run, and when
 * one throws the thread shows "That didn't work. Try again." and a failed
 * reply keeps its text. Success is announced through a polite live region.
 * Delete is offered on a comment with `canDelete`, by long press and as a
 * TalkBack action, and asks for confirmation first.
 *
 * @param comments the comments, oldest first. The first starts the thread; the rest are replies.
 * @param modifier the modifier of the thread.
 * @param quote the passage the thread is about, shown above it with a bar along the start edge.
 * @param resolved the thread is resolved: a Resolved badge, the subtle fill, and a hint instead of the reply field.
 * @param visibleReplies the latest replies shown before older ones fold.
 * @param now the current time in epoch milliseconds, for the relative times. `null` follows the clock.
 * @param replyState the reply text. Hoist it to keep a draft across screens.
 * @param labels replacements for the built-in strings, including the comments'.
 * @param onResolvedChange resolves (`true`) or reopens (`false`) the thread; shows the Resolve or Reopen button.
 * Throw to report a failure.
 * @param onReply posts a reply with the trimmed text; shows the reply field. Throw to keep the text and report a
 * failure.
 * @param onDeleteComment deletes the comment with this id, after confirmation; offered on comments with
 * `canDelete`. Throw to report a failure.
 * @param resolveIcon the icon before Resolve, a `MonarchIcon` with a `null` description. Text only without it.
 */
@Suppress("LongParameterList", "LongMethod", "CyclomaticComplexMethod")
@OptIn(ExperimentalLayoutApi::class)
@ExperimentalMonarchApi
@Composable
public fun MonarchCommentThread(
    comments: List<MonarchCommentThreadComment>,
    modifier: Modifier = Modifier,
    quote: String? = null,
    resolved: Boolean = false,
    visibleReplies: Int = MonarchCommentThreadTokens.DEFAULT_VISIBLE_REPLIES,
    now: Long? = null,
    replyState: TextFieldState = rememberTextFieldState(),
    labels: MonarchCommentThreadLabels = MonarchCommentThreadLabels(),
    onResolvedChange: (suspend (Boolean) -> Unit)? = null,
    onReply: (suspend (String) -> Unit)? = null,
    onDeleteComment: (suspend (String) -> Unit)? = null,
    resolveIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.Check,
) {
    val tokens = MonarchCommentThreadTokens
    val colors = MonarchTheme.colors
    val scope = rememberCoroutineScope()
    var expanded by rememberSaveable { mutableStateOf(false) }
    var revealed by remember { mutableStateOf(false) }
    var resolving by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<MonarchCommentThreadComment?>(null) }

    val strings = commentThreadStrings(labels)
    val root = comments.firstOrNull()
    val replies = comments.drop(1)
    val folded = tokens.folded(replies.size, visibleReplies, expanded)
    val shown = replies.drop(folded)

    val run: (suspend () -> Unit, () -> String) -> Unit = { work, success ->
        failed = false
        notice = ""
        scope.launch {
            failed = !commentThreadAttempt(work)
            if (!failed) notice = success()
        }
    }

    val threadName = root?.let { labels.label?.invoke(it.author) }
        ?: root?.let { stringResource(R.string.monarch_commentthread_label, it.author) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(tokens.background(resolved, colors))
            .padding(vertical = tokens.Padding)
            .semantics {
                isTraversalGroup = true
                if (threadName != null) contentDescription = threadName
            },
        verticalArrangement = Arrangement.spacedBy(tokens.Gap),
    ) {
        if (quote != null || onResolvedChange != null || resolved) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = tokens.InsetStart),
                horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
                verticalArrangement = Arrangement.spacedBy(tokens.Gap),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                if (quote != null) CommentThreadQuote(quote, labels)
                if (resolved) MonarchBadge(text = strings.resolved, tone = MonarchTone.Success)
                if (onResolvedChange != null) {
                    MonarchButton(
                        text = if (resolved) strings.reopen else strings.resolve,
                        onClick = {
                            if (!resolving) {
                                val target = !resolved
                                resolving = true
                                run({
                                    try {
                                        onResolvedChange(target)
                                    } finally {
                                        resolving = false
                                    }
                                }) { if (target) strings.resolvedNotice else strings.reopenedNotice }
                            }
                        },
                        modifier = Modifier.monarchGhostStartAlign(),
                        variant = MonarchButtonVariant.Ghost,
                        size = MonarchButtonSize.Sm,
                        loading = resolving,
                        leadingIcon = if (resolved) null else resolveIcon,
                    )
                }
            }
        }
        val requester = remember { FocusRequester() }
        LaunchedEffect(revealed) {
            if (revealed) {
                withFrameNanos { }
                requester.requestFocus()
            }
        }
        if (root != null) {
            CommentThreadComment(root, now, labels.messageRow, strings, onDeleteComment) { pendingDelete = it }
        }
        if (folded > 0) {
            MonarchButton(
                text = labels.showEarlier?.invoke(folded)
                    ?: pluralStringResource(R.plurals.monarch_commentthread_show_earlier, folded, folded),
                onClick = {
                    expanded = true
                    revealed = true
                },
                modifier = Modifier.padding(horizontal = tokens.InsetStart),
                variant = MonarchButtonVariant.Ghost,
                size = MonarchButtonSize.Sm,
            )
        }
        shown.forEachIndexed { index, comment ->
            val target = revealed && index == 0
            Box(
                if (target) {
                    Modifier
                        .focusRequester(requester)
                        .focusable()
                } else {
                    Modifier
                },
            ) {
                CommentThreadComment(comment, now, labels.messageRow, strings, onDeleteComment) { pendingDelete = it }
            }
        }
        CommentThreadReply(
            onReply = onReply,
            resolved = resolved,
            replyState = replyState,
            strings = strings,
            onStart = {
                failed = false
                notice = ""
            },
            onResult = { ok ->
                failed = !ok
                if (ok) notice = strings.sentNotice
            },
        )
        // Always present, so a change is announced. A failure, or the confirmation of the last action.
        Box(
            Modifier
                .padding(horizontal = tokens.InsetStart)
                .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        ) {
            when {
                failed -> MonarchText(
                    text = strings.actionFailed,
                    style = MonarchTextStyle.Caption,
                    color = tokens.errorColor(colors),
                )
                notice.isNotEmpty() -> MonarchText(
                    text = notice,
                    style = MonarchTextStyle.Caption,
                    color = tokens.mutedColor(colors),
                )
            }
        }
    }

    pendingDelete?.let { comment ->
        CommentThreadDeleteDialog(
            strings = strings,
            onDismiss = { pendingDelete = null },
            onConfirm = {
                pendingDelete = null
                val delete = onDeleteComment ?: return@CommentThreadDeleteDialog
                run({ delete(comment.id) }) { "" }
            },
        )
    }
}

/** Runs [work]; `false` when it threw anything but a cancellation, which the thread reports as a failure. */
@Suppress("TooGenericExceptionCaught", "SwallowedException")
internal suspend fun commentThreadAttempt(work: suspend () -> Unit): Boolean = try {
    work()
    true
} catch (cancel: CancellationException) {
    throw cancel
} catch (error: Exception) {
    false
}

/** The built-in strings that are plain text, resolved once. */
@Suppress("LongParameterList")
private class CommentThreadStrings(
    val reply: String,
    val replyPlaceholder: String,
    val send: String,
    val resolve: String,
    val reopen: String,
    val resolved: String,
    val resolvedHint: String,
    val resolvedNotice: String,
    val reopenedNotice: String,
    val sentNotice: String,
    val actionFailed: String,
    val deleteComment: String,
    val deleteTitle: String,
    val deleteDescription: String,
    val deleteConfirm: String,
    val cancel: String,
)

@Composable
private fun commentThreadStrings(labels: MonarchCommentThreadLabels) = CommentThreadStrings(
    reply = monarchString(labels.reply, R.string.monarch_commentthread_reply),
    replyPlaceholder = monarchString(labels.replyPlaceholder, R.string.monarch_commentthread_reply_placeholder),
    send = monarchString(labels.send, R.string.monarch_commentthread_send),
    resolve = monarchString(labels.resolve, R.string.monarch_commentthread_resolve),
    reopen = monarchString(labels.reopen, R.string.monarch_commentthread_reopen),
    resolved = monarchString(labels.resolved, R.string.monarch_commentthread_resolved),
    resolvedHint = monarchString(labels.resolvedHint, R.string.monarch_commentthread_resolved_hint),
    resolvedNotice = monarchString(labels.resolvedNotice, R.string.monarch_commentthread_resolved_notice),
    reopenedNotice = monarchString(labels.reopenedNotice, R.string.monarch_commentthread_reopened_notice),
    sentNotice = monarchString(labels.sentNotice, R.string.monarch_commentthread_sent_notice),
    actionFailed = monarchString(labels.actionFailed, R.string.monarch_commentthread_action_failed),
    deleteComment = monarchString(labels.deleteComment, R.string.monarch_commentthread_delete_comment),
    deleteTitle = monarchString(labels.deleteTitle, R.string.monarch_commentthread_delete_title),
    deleteDescription = monarchString(labels.deleteDescription, R.string.monarch_commentthread_delete_description),
    deleteConfirm = monarchString(labels.deleteConfirm, R.string.monarch_commentthread_delete_confirm),
    cancel = monarchString(labels.cancel, R.string.monarch_commentthread_cancel),
)

/** The quote, with a bar along the start edge, read as "Quoted text: …". */
@Composable
private fun CommentThreadQuote(quote: String, labels: MonarchCommentThreadLabels) {
    val tokens = MonarchCommentThreadTokens
    val colors = MonarchTheme.colors
    val spoken = labels.quote?.invoke(quote) ?: stringResource(R.string.monarch_commentthread_quote, quote)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clearAndSetSemantics { contentDescription = spoken },
        horizontalArrangement = Arrangement.spacedBy(tokens.QuoteGap),
    ) {
        Box(
            Modifier
                .width(tokens.QuoteBarWidth)
                .fillMaxHeight()
                .background(tokens.quoteBarColor(colors)),
        )
        MonarchText(text = quote, style = MonarchTextStyle.Body, color = tokens.mutedColor(colors))
    }
}

/** One comment as a `MonarchMessageRow`, with Delete when allowed. */
@OptIn(ExperimentalMonarchApi::class)
@Composable
private fun CommentThreadComment(
    comment: MonarchCommentThreadComment,
    now: Long?,
    rowLabels: MonarchMessageRowLabels,
    strings: CommentThreadStrings,
    onDeleteComment: (suspend (String) -> Unit)?,
    onAskDelete: (MonarchCommentThreadComment) -> Unit,
) {
    val actions = if (onDeleteComment != null && comment.canDelete) {
        listOf(MonarchMessageRowAction(strings.deleteComment, onClick = { onAskDelete(comment) }, destructive = true))
    } else {
        emptyList()
    }
    // Delete is also a visible button, so it can be found without a long press.
    Row(verticalAlignment = Alignment.Top) {
        MonarchMessageRow(
            author = MonarchMessageRowAuthor(comment.author, comment.authorImage, comment.authorSeed),
            time = comment.createdAt,
            now = now,
            actions = actions,
            labels = rowLabels,
            modifier = Modifier.weight(1f),
        ) { MonarchText(text = comment.body) }
        if (actions.isNotEmpty()) {
            MonarchIconButton(
                label = strings.deleteComment,
                onClick = { onAskDelete(comment) },
                size = MonarchButtonSize.Sm,
                icon = MonarchDefaultIcons.Trash,
            )
        }
    }
}

/** The reply field while open, the resolved hint while resolved, nothing without [onReply]. */
@Suppress("TooGenericExceptionCaught")
@OptIn(ExperimentalMonarchApi::class)
@Composable
private fun CommentThreadReply(
    onReply: (suspend (String) -> Unit)?,
    resolved: Boolean,
    replyState: TextFieldState,
    strings: CommentThreadStrings,
    onStart: () -> Unit,
    onResult: (Boolean) -> Unit,
) {
    val tokens = MonarchCommentThreadTokens
    if (onReply == null) return
    if (!tokens.canReply(takesReplies = true, resolved = resolved)) {
        MonarchText(
            text = strings.resolvedHint,
            modifier = Modifier.padding(horizontal = tokens.InsetStart),
            style = MonarchTextStyle.Caption,
            color = tokens.mutedColor(MonarchTheme.colors),
        )
        return
    }
    MonarchMessageComposer(
        label = strings.reply,
        state = replyState,
        onSend = { message ->
            onStart()
            try {
                onReply(message.text)
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                onResult(false)
                // The composer keeps the text when sending throws.
                throw error
            }
            onResult(true)
        },
        modifier = Modifier.padding(horizontal = tokens.InsetStart),
        placeholder = strings.replyPlaceholder,
        labels = MonarchMessageComposerLabels(send = strings.send),
    )
}

@Composable
private fun CommentThreadDeleteDialog(strings: CommentThreadStrings, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    MonarchDialog(
        onDismissRequest = onDismiss,
        title = strings.deleteTitle,
        description = strings.deleteDescription,
        actions = {
            MonarchButton(text = strings.cancel, onClick = onDismiss, variant = MonarchButtonVariant.Ghost)
            MonarchButton(text = strings.deleteConfirm, onClick = onConfirm, variant = MonarchButtonVariant.Danger)
        },
    )
}
