package com.rookhq.monarch.components.collaboration.threadpanel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.iconbutton.MonarchIconButton
import com.rookhq.monarch.components.collaboration.virtualmessagelist.MonarchVirtualMessageList
import com.rookhq.monarch.components.collaboration.virtualmessagelist.MonarchVirtualMessageListContext
import com.rookhq.monarch.components.collaboration.virtualmessagelist.MonarchVirtualMessageListLabels
import com.rookhq.monarch.components.datadisplay.tag.MonarchChip
import com.rookhq.monarch.components.feedback.inlineerrorretry.MonarchInlineErrorRetry
import com.rookhq.monarch.components.feedback.loadingstate.MonarchLoadingState
import com.rookhq.monarch.components.layout.divider.MonarchDivider
import com.rookhq.monarch.components.layout.divider.MonarchDividerVariant
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.internal.MonarchAnnouncerNode
import com.rookhq.monarch.internal.MonarchDefaultIcons
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.internal.rememberMonarchAnnouncer
import com.rookhq.monarch.theme.MonarchTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * The panel of one conversation thread: the message it answers, a reply count divider, the replies pinned to
 * the newest one, and the reply box.
 *
 * Built by Monarch for a Material 3 `ModalBottomSheet` on a phone or a supporting pane beside the conversation
 * at expanded width: a heading with a selectable `MonarchChip` to follow the thread, the app's [headerActions]
 * and an optional Close button; the replies in a `MonarchVirtualMessageList`, which keeps the newest reply in
 * view while the reader is at the bottom, adds day separators and groups consecutive replies, with [root] and
 * the divider above the first reply; and the app's [typing] indicator and [composer], usually a
 * `MonarchTypingIndicator` and a `MonarchMessageComposer`, pinned under the list. [status] replaces the
 * replies with placeholders, an error with Try again, or a deleted notice with Close.
 *
 * Following is a suspending function: the chip is disabled while it runs, and the result or the failure is
 * announced through a polite live region that is always present. [onRepliesSeen] is called once the replies
 * have been on screen for [seenDelayMillis], the moment to mark the thread read. Give the panel a bounded
 * height, as for any lazy list.
 *
 * @param T the type of the replies.
 * @param replies the loaded replies, oldest first.
 * @param key a stable, unique key per reply, as for `LazyColumn`.
 * @param time when a reply was sent, in epoch milliseconds (UTC), for day separators and grouping.
 * @param modifier the modifier of the panel.
 * @param replyCount the number of replies, for the divider; more than [replies] when not all are loaded.
 * @param status what the body shows.
 * @param following the person follows the thread and is notified about replies.
 * @param state the scroll state of the replies. Index 0 is the bottom row.
 * @param authorId the author of a reply, for grouping consecutive replies. `null` never groups.
 * @param notice a note after the replies, such as "Showing the first 500 replies."
 * @param seenDelayMillis how long the replies stay on screen before [onRepliesSeen].
 * @param now the current time, for Today and Yesterday. `null` reads the clock.
 * @param locale the locale of the day separators. `null` uses the configuration's.
 * @param timeZone the time zone of the day separators. An IANA id such as `"Europe/Berlin"`. `null`, or an id the
 * device does not know, is the device's zone.
 * @param labels replacements for the built-in strings.
 * @param onClose shows a Close button in the header, and with a deleted thread.
 * @param onRetry shows Try again when the thread could not be loaded.
 * @param onFollowingChange follows (`true`) or unfollows (`false`) the thread; shows the follow chip. Throw to
 * report a failure.
 * @param onRepliesSeen called with [replyCount] once the replies have been on screen for [seenDelayMillis], and
 * again when the count changes.
 * @param followIcon the icon of the follow chip, a bell `MonarchIcon` with a `null` description.
 * @param headerActions further header controls, before Close.
 * @param root the message the thread answers, usually a `MonarchMessageRow`.
 * @param typing above the reply box, such as a `MonarchTypingIndicator`.
 * @param composer the reply box, usually a `MonarchMessageComposer`, pinned to the bottom of the panel.
 * @param reply draws one reply, usually a `MonarchMessageRow` with `grouped = context.grouped`.
 */
@Suppress("LongParameterList", "LongMethod")
@ExperimentalMonarchApi
@Composable
public fun <T> MonarchThreadPanel(
    replies: List<T>,
    key: (reply: T) -> Any,
    time: (reply: T) -> Long,
    modifier: Modifier = Modifier,
    replyCount: Int = replies.size,
    status: MonarchThreadPanelStatus = MonarchThreadPanelStatus.Ready,
    following: Boolean = false,
    state: LazyListState = rememberLazyListState(),
    authorId: (reply: T) -> String? = { null },
    notice: String? = null,
    seenDelayMillis: Long = MonarchThreadPanelTokens.SEEN_DELAY_MILLIS,
    now: Long? = null,
    locale: Locale? = null,
    timeZone: String? = null,
    labels: MonarchThreadPanelLabels = MonarchThreadPanelLabels(),
    onClose: (() -> Unit)? = null,
    onRetry: (() -> Unit)? = null,
    onFollowingChange: (suspend (Boolean) -> Unit)? = null,
    onRepliesSeen: ((replyCount: Int) -> Unit)? = null,
    followIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.Bell,
    headerActions: (@Composable RowScope.() -> Unit)? = null,
    root: (@Composable () -> Unit)? = null,
    typing: (@Composable () -> Unit)? = null,
    composer: (@Composable () -> Unit)? = null,
    reply: @Composable LazyItemScope.(reply: T, context: MonarchVirtualMessageListContext<T>) -> Unit,
) {
    val tokens = MonarchThreadPanelTokens
    val colors = MonarchTheme.colors
    val announcer = rememberMonarchAnnouncer()
    val ready = status == MonarchThreadPanelStatus.Ready

    // The callback is read when the delay ends, so a new lambda does not restart it.
    val seen by rememberUpdatedState(onRepliesSeen)
    val wantsSeen = onRepliesSeen != null
    LaunchedEffect(wantsSeen, ready, replyCount, seenDelayMillis) {
        if (wantsSeen && ready) {
            delay(seenDelayMillis)
            seen?.invoke(replyCount)
        }
    }

    Column(modifier = modifier) {
        MonarchThreadPanelHeader(
            title = monarchString(labels.title, R.string.monarch_threadpanel_title),
            following = following,
            showFollow = tokens.showsFollow(onFollowingChange != null, status),
            onFollowingChange = onFollowingChange,
            onAnnounce = announcer::announce,
            labels = labels,
            onClose = onClose,
            followIcon = followIcon,
            headerActions = headerActions,
        )
        MonarchDivider(variant = MonarchDividerVariant.Subtle)
        Box(modifier = Modifier.weight(1f, fill = false).fillMaxWidth()) {
            when (status) {
                MonarchThreadPanelStatus.Loading -> MonarchLoadingState(
                    label = monarchString(labels.loading, R.string.monarch_threadpanel_loading),
                    modifier = Modifier.padding(tokens.Inset),
                    rows = tokens.PLACEHOLDER_ROWS,
                )
                MonarchThreadPanelStatus.Error -> MonarchThreadPanelError(
                    message = monarchString(labels.error, R.string.monarch_threadpanel_error),
                    onRetry = onRetry,
                )
                MonarchThreadPanelStatus.Deleted -> MonarchThreadPanelDeleted(
                    message = monarchString(labels.deleted, R.string.monarch_threadpanel_deleted),
                    close = monarchString(labels.close, R.string.monarch_threadpanel_close),
                    onClose = onClose,
                )
                MonarchThreadPanelStatus.Ready -> MonarchVirtualMessageList(
                    items = replies,
                    key = key,
                    time = time,
                    modifier = Modifier.fillMaxWidth(),
                    state = state,
                    authorId = authorId,
                    now = now,
                    locale = locale,
                    timeZone = timeZone,
                    labels = labels.list ?: MonarchVirtualMessageListLabels(
                        label = monarchString(labels.replies, R.string.monarch_threadpanel_replies),
                    ),
                    header = { MonarchThreadPanelRoot(root, replyCount, labels) },
                    footer = notice?.let { text ->
                        {
                            MonarchText(
                                text = text,
                                modifier = Modifier.padding(horizontal = tokens.Inset, vertical = tokens.Gap),
                                style = MonarchTextStyle.Caption,
                                color = tokens.mutedColor(colors),
                            )
                        }
                    },
                    row = reply,
                )
            }
        }
        if (typing != null || composer != null) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = tokens.Inset, vertical = tokens.Gap),
                verticalArrangement = Arrangement.spacedBy(tokens.Gap),
            ) {
                typing?.invoke()
                composer?.invoke()
            }
        }
        // Always present, so a follow change and its failure are announced as a change of this region.
        MonarchAnnouncerNode(announcer)
    }
}

/** The heading, the follow chip, the app's actions and Close. */
@Suppress("LongParameterList")
@Composable
private fun MonarchThreadPanelHeader(
    title: String,
    following: Boolean,
    showFollow: Boolean,
    onFollowingChange: (suspend (Boolean) -> Unit)?,
    onAnnounce: (String) -> Unit,
    labels: MonarchThreadPanelLabels,
    onClose: (() -> Unit)?,
    followIcon: (@Composable () -> Unit)?,
    headerActions: (@Composable RowScope.() -> Unit)?,
) {
    val tokens = MonarchThreadPanelTokens
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = tokens.Inset, end = tokens.Gap),
        horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f).semantics { heading() },
            style = MonarchTheme.typography.subheading,
            color = tokens.titleColor(MonarchTheme.colors),
        )
        if (showFollow && onFollowingChange != null) {
            MonarchThreadPanelFollow(following, onFollowingChange, onAnnounce, labels, followIcon)
        }
        headerActions?.invoke(this)
        if (onClose != null) {
            MonarchIconButton(
                label = monarchString(labels.close, R.string.monarch_threadpanel_close),
                onClick = onClose,
                size = MonarchButtonSize.Sm,
            ) {
                MonarchDefaultIcons.Close()
            }
        }
    }
}

/** The follow chip: a selectable `MonarchChip`, disabled while the change runs, with the result announced. */
@Suppress("TooGenericExceptionCaught", "SwallowedException")
@Composable
private fun MonarchThreadPanelFollow(
    following: Boolean,
    onFollowingChange: suspend (Boolean) -> Unit,
    onAnnounce: (String) -> Unit,
    labels: MonarchThreadPanelLabels,
    followIcon: (@Composable () -> Unit)?,
) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    val followed = monarchString(labels.followedNotice, R.string.monarch_threadpanel_followed_notice)
    val unfollowed = monarchString(labels.unfollowedNotice, R.string.monarch_threadpanel_unfollowed_notice)
    val failed = monarchString(labels.followFailed, R.string.monarch_threadpanel_follow_failed)
    MonarchChip(
        text = if (following) {
            monarchString(labels.following, R.string.monarch_threadpanel_following)
        } else {
            monarchString(labels.follow, R.string.monarch_threadpanel_follow)
        },
        selected = following,
        onSelectedChange = {
            if (!busy) {
                val next = !following
                busy = true
                onAnnounce("")
                scope.launch {
                    val succeeded = try {
                        onFollowingChange(next)
                        true
                    } catch (cancel: CancellationException) {
                        throw cancel
                    } catch (error: Exception) {
                        false
                    } finally {
                        busy = false
                    }
                    onAnnounce(MonarchThreadPanelTokens.followNotice(succeeded, next, followed, unfollowed, failed))
                }
            }
        },
        enabled = !busy,
        leadingIcon = followIcon,
    )
}

/** The root message and the reply count divider, above the first reply. */
@Composable
private fun MonarchThreadPanelRoot(root: (@Composable () -> Unit)?, replyCount: Int, labels: MonarchThreadPanelLabels) {
    val tokens = MonarchThreadPanelTokens
    val colors = MonarchTheme.colors
    Column(modifier = Modifier.fillMaxWidth().padding(top = tokens.Gap)) {
        root?.invoke()
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = tokens.Inset, vertical = tokens.Gap),
            horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MonarchText(
                text = labels.replyCount?.invoke(replyCount)
                    ?: pluralStringResource(R.plurals.monarch_threadpanel_reply_count, replyCount, replyCount),
                style = MonarchTextStyle.Caption,
                color = tokens.mutedColor(colors),
            )
            MonarchDivider(modifier = Modifier.weight(1f), variant = MonarchDividerVariant.Subtle)
        }
    }
}

/** A loading error, with Try again when the app can retry. */
@Composable
private fun MonarchThreadPanelError(message: String, onRetry: (() -> Unit)?) {
    val tokens = MonarchThreadPanelTokens
    if (onRetry != null) {
        MonarchInlineErrorRetry(message = message, onRetry = onRetry, modifier = Modifier.padding(tokens.Inset))
    } else {
        MonarchText(
            text = message,
            modifier = Modifier
                .padding(tokens.Inset)
                .semantics { liveRegion = LiveRegionMode.Polite },
            color = tokens.errorColor(MonarchTheme.colors),
        )
    }
}

/** The deleted notice, announced, with Close. */
@Composable
private fun MonarchThreadPanelDeleted(message: String, close: String, onClose: (() -> Unit)?) {
    val tokens = MonarchThreadPanelTokens
    Column(
        modifier = Modifier.fillMaxWidth().padding(tokens.Inset),
        verticalArrangement = Arrangement.spacedBy(tokens.Gap),
    ) {
        MonarchText(
            text = message,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            color = tokens.mutedColor(MonarchTheme.colors),
        )
        if (onClose != null) {
            MonarchButton(text = close, onClick = onClose, size = MonarchButtonSize.Sm)
        }
    }
}
