package com.rookhq.monarch.components.collaboration.messagerow

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.R
import com.rookhq.monarch.accessibility.monarchMinimumTouchTarget
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.collaboration.reactionbar.MonarchReactionBar
import com.rookhq.monarch.components.collaboration.reactionbar.MonarchReactionBarItem
import com.rookhq.monarch.components.datadisplay.badge.MonarchBadge
import com.rookhq.monarch.components.identity.avatar.MonarchAvatar
import com.rookhq.monarch.components.overlays.menu.MonarchMenu
import com.rookhq.monarch.components.overlays.menu.MonarchMenuItem
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.internal.currentIcuTimeZone
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.internal.rememberMonarchRelativeClock
import com.rookhq.monarch.theme.MonarchTheme

/**
 * One message in a conversation: avatar, author, time and body, with
 * pending, failed, edited and deleted states, reactions, a thread reply count
 * and actions on long press.
 *
 * A Monarch pattern built from `MonarchAvatar`, `MonarchBadge`,
 * `MonarchReactionBar`, `MonarchButton` and a `MonarchMenu`.
 * The author, tags, full date and time, body, edited marker and "Sending…"
 * are one TalkBack element; a grouped row, which hides the avatar and header,
 * still begins with the author and the date for TalkBack. The [actions] open
 * in a menu on long press, with a haptic, and are also TalkBack actions. A
 * failure is written as text, with Retry and Discard as separate buttons, and
 * is announced through a polite live region.
 *
 * Without [now], the relative time refreshes every minute.
 *
 * @param author the author's name and avatar picture.
 * @param time when the message was sent, in epoch milliseconds (UTC).
 * @param modifier the modifier of the row.
 * @param now the current time in epoch milliseconds, for tests and previews. `null` follows the clock.
 * @param variant a message, an internal note or a system line.
 * @param status sent, pending or failed.
 * @param timeStyle a relative time or the time of day.
 * @param locale the locale of the time (a 24-hour clock for `en-GB`). `null` uses the first locale of the
 *   configuration.
 * @param timeZone the id of the zone the time of day is shown in ("UTC"). `null` uses the device zone.
 * @param errorMessage why sending failed. "Not sent" by default.
 * @param tags small neutral badges after the author's name, such as Internal or Guest.
 * @param reactions emoji reactions, shown with `MonarchReactionBar` under the body.
 * @param replyCount the number of thread replies; shown when above zero.
 * @param grouped a follow-up from the same author: no avatar or header, the body lines up with the first message.
 * @param edited shows the edited marker.
 * @param deleted replaces the body with "This message was deleted." and hides reactions, replies and actions.
 * @param highlighted tints the row with the subtle accent, for example after opening a link to the message.
 * @param actions the actions of a sent message, offered by long press and as TalkBack actions.
 * @param quickReactions emoji offered by the reaction bar's add button when no [reactionPicker] is given.
 * @param reactionPicker the app's emoji picker for the reaction bar's add button. See `MonarchReactionBar`.
 * @param labels replacements for the built-in strings.
 * @param onRetry adds Retry to a failed message.
 * @param onDiscard adds Discard to a failed message.
 * @param onShowEdits makes the edited marker a button that opens the edit history.
 * @param onReactionToggle receives the emoji and its new state. Without it the reactions are read-only.
 * @param onOpenThread makes the reply count a button that opens the thread.
 * @param content the message body: text, attachments, link cards. Use `MonarchText` with the body style.
 */
@Suppress("LongParameterList")
@Composable
public fun MonarchMessageRow(
    author: MonarchMessageRowAuthor,
    time: Long,
    modifier: Modifier = Modifier,
    now: Long? = null,
    variant: MonarchMessageRowVariant = MonarchMessageRowVariant.Standard,
    status: MonarchMessageRowStatus = MonarchMessageRowStatus.Sent,
    timeStyle: MonarchMessageRowTimeStyle = MonarchMessageRowTimeStyle.Relative,
    locale: java.util.Locale? = null,
    timeZone: String? = null,
    errorMessage: String? = null,
    tags: List<String> = emptyList(),
    reactions: List<MonarchReactionBarItem> = emptyList(),
    replyCount: Int = 0,
    grouped: Boolean = false,
    edited: Boolean = false,
    deleted: Boolean = false,
    highlighted: Boolean = false,
    actions: List<MonarchMessageRowAction> = emptyList(),
    quickReactions: List<String> = emptyList(),
    reactionPicker: (@Composable (onPick: (String) -> Unit, onDismiss: () -> Unit) -> Unit)? = null,
    labels: MonarchMessageRowLabels = MonarchMessageRowLabels(),
    onRetry: (() -> Unit)? = null,
    onDiscard: (() -> Unit)? = null,
    onShowEdits: (() -> Unit)? = null,
    onReactionToggle: ((emoji: String, reacted: Boolean) -> Unit)? = null,
    onOpenThread: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val times = messageRowTimes(time, now, timeStyle, locale, timeZone)
    if (variant == MonarchMessageRowVariant.System) {
        SystemLine(times, modifier, content)
        return
    }
    val colors = MonarchTheme.colors
    val tokens = MonarchMessageRowTokens
    val background by animateColorAsState(
        targetValue = tokens.background(variant, highlighted, colors),
        animationSpec = MonarchTheme.motion.spec(),
        label = "message row background",
    )
    val bar = tokens.bar(variant, colors)
    val offersActions = tokens.offersActions(variant, status, deleted, actions.size)
    var menu by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val actionsLabel = monarchString(labels.actions, R.string.monarch_messagerow_actions)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(background)
            .drawBehind {
                if (bar != null) {
                    val width = tokens.InternalBarWidth.toPx()
                    val x = if (layoutDirection == LayoutDirection.Rtl) size.width - width else 0f
                    drawRect(bar, topLeft = Offset(x, 0f), size = Size(width, size.height))
                }
            }
            .messageRowActions(offersActions, actions, actionsLabel) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                menu = true
            },
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = tokens.HorizontalPadding,
                vertical = if (grouped) tokens.GroupedVerticalPadding else tokens.VerticalPadding,
            ),
            horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
        ) {
            if (grouped) {
                Spacer(Modifier.width(tokens.AvatarSide))
            } else {
                MonarchAvatar(
                    image = author.image,
                    name = author.name,
                    size = tokens.AvatarSize,
                    seed = author.seed,
                    decorative = true,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(tokens.StackGap),
            ) {
                MessageBlock(author, tags, times, grouped, status, edited, deleted, labels, onShowEdits, content)
                FailureLine(status, errorMessage, labels, onRetry, onDiscard)
                if (!deleted) {
                    MonarchReactionBar(
                        reactions = reactions,
                        onToggle = onReactionToggle,
                        quickReactions = quickReactions,
                        picker = reactionPicker,
                        labels = labels.reactions,
                    )
                    Replies(replyCount, labels, onOpenThread)
                }
            }
        }
        if (offersActions) ActionMenu(menu, actions) { menu = false }
    }
}

/** The visible time and the full date and time TalkBack reads. */
private class MessageRowTimes(val visible: String, val spoken: String)

@Composable
private fun messageRowTimes(
    time: Long,
    now: Long?,
    style: MonarchMessageRowTimeStyle,
    localeOverride: java.util.Locale?,
    zoneId: String?,
): MessageRowTimes {
    val locale = localeOverride ?: LocalConfiguration.current.locales[0]
    val zone = zoneId?.let { android.icu.util.TimeZone.getTimeZone(it) } ?: currentIcuTimeZone()
    val visible = when (style) {
        MonarchMessageRowTimeStyle.Relative -> {
            val clock = rememberMonarchRelativeClock(listOf(time), fixed = now)
            MonarchMessageRowTokens.relativeTime(time, clock, locale)
        }
        MonarchMessageRowTimeStyle.Clock -> MonarchMessageRowTokens.clockTime(time, locale, zone)
    }
    return MessageRowTimes(
        visible,
        MonarchMessageRowTokens.fullTime(time, locale, zone),
    )
}

/** The long press and the TalkBack actions. */
private fun Modifier.messageRowActions(
    offered: Boolean,
    actions: List<MonarchMessageRowAction>,
    label: String,
    open: () -> Unit,
): Modifier {
    if (!offered) return this
    return this
        .pointerInput(actions) { detectTapGestures(onLongPress = { open() }) }
        .semantics {
            onLongClick(label) {
                open()
                true
            }
            customActions = actions.map { action ->
                CustomAccessibilityAction(action.label) {
                    action.onClick()
                    true
                }
            }
        }
}

/** The header, the body and the markers, read as one element. */
@OptIn(ExperimentalLayoutApi::class)
@Suppress("LongParameterList")
@Composable
private fun MessageBlock(
    author: MonarchMessageRowAuthor,
    tags: List<String>,
    times: MessageRowTimes,
    grouped: Boolean,
    status: MonarchMessageRowStatus,
    edited: Boolean,
    deleted: Boolean,
    labels: MonarchMessageRowLabels,
    onShowEdits: (() -> Unit)?,
    content: @Composable () -> Unit,
) {
    val colors = MonarchTheme.colors
    val tokens = MonarchMessageRowTokens
    val separator = stringResource(R.string.monarch_messagerow_separator)
    val spoken = (listOf(author.name) + tags + times.spoken).joinToString(separator)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { },
        verticalArrangement = Arrangement.spacedBy(tokens.StackGap),
    ) {
        if (grouped) {
            // Not drawn: TalkBack still hears who wrote a follow-up and when.
            Box(Modifier.clearAndSetSemantics { contentDescription = spoken })
        } else {
            FlowRow(
                modifier = Modifier.clearAndSetSemantics { contentDescription = spoken },
                horizontalArrangement = Arrangement.spacedBy(tokens.HeaderGap),
                verticalArrangement = Arrangement.spacedBy(tokens.StackGap),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                MonarchText(text = author.name, style = MonarchTextStyle.BodyStrong)
                tags.forEach { MonarchBadge(text = it) }
                MonarchText(text = times.visible, style = MonarchTextStyle.Caption, color = tokens.mutedColor(colors))
            }
        }
        CompositionLocalProvider(
            LocalContentColor provides tokens.bodyColor(status, deleted, colors),
            LocalTextStyle provides MonarchTheme.typography.body,
        ) {
            if (deleted) {
                Text(
                    text = monarchString(labels.deleted, R.string.monarch_messagerow_deleted),
                    style = MonarchTheme.typography.body.copy(fontStyle = FontStyle.Italic),
                )
            } else {
                content()
            }
        }
        if (edited && !deleted) EditedMarker(labels, onShowEdits)
        if (status == MonarchMessageRowStatus.Pending) {
            MonarchText(
                text = monarchString(labels.sending, R.string.monarch_messagerow_sending),
                style = MonarchTextStyle.Caption,
                color = tokens.mutedColor(colors),
            )
        }
    }
}

@Composable
private fun EditedMarker(labels: MonarchMessageRowLabels, onShowEdits: (() -> Unit)?) {
    val text = monarchString(labels.edited, R.string.monarch_messagerow_edited)
    val color = MonarchMessageRowTokens.mutedColor(MonarchTheme.colors)
    if (onShowEdits == null) {
        MonarchText(text = text, style = MonarchTextStyle.Caption, color = color)
        return
    }
    val name = monarchString(labels.editHistory, R.string.monarch_messagerow_edit_history)
    Box(
        modifier = Modifier
            .monarchMinimumTouchTarget()
            .clickable(role = Role.Button, onClick = onShowEdits)
            .semantics { contentDescription = name },
        contentAlignment = Alignment.CenterStart,
    ) {
        MonarchText(
            text = text,
            modifier = Modifier.clearAndSetSemantics { },
            style = MonarchTextStyle.Caption,
            color = MonarchTheme.colors.textLink,
        )
    }
}

/** The failure, announced when it appears. The live region is always present so the change is heard. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FailureLine(
    status: MonarchMessageRowStatus,
    errorMessage: String?,
    labels: MonarchMessageRowLabels,
    onRetry: (() -> Unit)?,
    onDiscard: (() -> Unit)?,
) {
    val failed = status == MonarchMessageRowStatus.Failed
    val tokens = MonarchMessageRowTokens
    Column(verticalArrangement = Arrangement.spacedBy(tokens.StackGap)) {
        Box(Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }) {
            if (failed) {
                MonarchText(
                    text = errorMessage ?: monarchString(labels.failed, R.string.monarch_messagerow_failed),
                    style = MonarchTextStyle.Caption,
                    color = tokens.errorColor(MonarchTheme.colors),
                )
            }
        }
        if (failed && (onRetry != null || onDiscard != null)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(tokens.HeaderGap)) {
                onRetry?.let {
                    MonarchButton(
                        text = monarchString(labels.retry, R.string.monarch_messagerow_retry),
                        onClick = it,
                        variant = MonarchButtonVariant.Ghost,
                        size = MonarchButtonSize.Sm,
                    )
                }
                onDiscard?.let {
                    MonarchButton(
                        text = monarchString(labels.discard, R.string.monarch_messagerow_discard),
                        onClick = it,
                        variant = MonarchButtonVariant.Ghost,
                        size = MonarchButtonSize.Sm,
                    )
                }
            }
        }
    }
}

@Composable
private fun Replies(count: Int, labels: MonarchMessageRowLabels, onOpenThread: (() -> Unit)?) {
    if (count <= 0) return
    val text = labels.replies?.invoke(count)
        ?: pluralStringResource(R.plurals.monarch_messagerow_replies, count, count)
    if (onOpenThread != null) {
        MonarchButton(
            text = text,
            onClick = onOpenThread,
            variant = MonarchButtonVariant.Link,
            size = MonarchButtonSize.Sm,
        )
    } else {
        MonarchText(
            text = text,
            style = MonarchTextStyle.Caption,
            color = MonarchMessageRowTokens.mutedColor(MonarchTheme.colors),
        )
    }
}

@Composable
private fun ActionMenu(expanded: Boolean, actions: List<MonarchMessageRowAction>, onDismiss: () -> Unit) {
    MonarchMenu(expanded = expanded, onDismissRequest = onDismiss) {
        actions.forEach { action ->
            MonarchMenuItem(
                text = action.label,
                onClick = {
                    onDismiss()
                    action.onClick()
                },
                destructive = action.destructive,
                leadingIcon = action.icon,
            )
        }
    }
}

/** A centered, muted line written by the server, with its time. */
@Composable
private fun SystemLine(times: MessageRowTimes, modifier: Modifier, content: @Composable () -> Unit) {
    val muted = MonarchMessageRowTokens.mutedColor(MonarchTheme.colors)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = MonarchMessageRowTokens.HorizontalPadding,
                vertical = MonarchMessageRowTokens.VerticalPadding,
            )
            .semantics(mergeDescendants = true) { },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MonarchMessageRowTokens.StackGap),
    ) {
        CompositionLocalProvider(
            LocalContentColor provides muted,
            LocalTextStyle provides MonarchTheme.typography.caption.copy(textAlign = TextAlign.Center),
        ) { content() }
        MonarchText(
            text = times.visible,
            modifier = Modifier.clearAndSetSemantics { contentDescription = times.spoken },
            style = MonarchTextStyle.Caption,
            color = muted,
        )
    }
}
