package com.rookhq.monarch.components.collaboration.inboxrow

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.R
import com.rookhq.monarch.components.datadisplay.listrow.MonarchListRow
import com.rookhq.monarch.components.forms.checkbox.MonarchCheckbox
import com.rookhq.monarch.components.identity.avatar.MonarchAvatar
import com.rookhq.monarch.components.identity.avatar.MonarchAvatarSize
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.internal.field.monarchFieldTextStyle
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.internal.rememberMonarchClock
import com.rookhq.monarch.theme.MonarchTheme
import com.rookhq.monarch.tokens.MonarchTokens
import kotlinx.coroutines.launch

/**
 * One conversation in an inbox: avatar, name, compact age, a one-line
 * preview, badges, an unread dot or count, and swipe actions.
 *
 * Built on `MonarchListRow`, with the Material 3 `SwipeToDismissBox` for the swipe
 * actions. The row is one TalkBack element read as the title, the unread
 * state ("3 unread"), the channel, the full date and time, the preview, then
 * the badges and trailing content. The swipe actions are also TalkBack
 * actions. With [onSelectedChange] a long press selects the row for bulk
 * actions, and while [selecting] a tap does too; a checkbox replaces the
 * avatar. The age ("5m", "3h", then a short date after 30 days) refreshes
 * every minute unless [now] is given.
 *
 * @param title who or what the conversation is with.
 * @param onClick opens the conversation.
 * @param modifier the modifier of the row.
 * @param unread the row is unread: a dot before a semibold title.
 * @param unreadCount the number of unread messages, shown as a count (99+ above 99) instead of the dot.
 * @param selected the row is selected for bulk actions.
 * @param current the row is the conversation open beside the list: a subtle accent fill and a bar at its start.
 * @param selecting the list is in selection mode: a tap selects instead of opening, and checkboxes show.
 * @param preview the latest message or a summary, on one line.
 * @param timestamp when the conversation last changed, in epoch milliseconds (UTC).
 * @param now the current time in epoch milliseconds, for tests. `null` uses the clock.
 * @param avatarName the name for the avatar's initials; [title] by default.
 * @param avatarImage the avatar picture, loaded by the app. `null` shows initials.
 * @param showAvatar shows the avatar.
 * @param icon a small icon after the title, such as the channel. Decorative; describe it in [iconLabel].
 * @param iconLabel the spoken description of [icon], "via email".
 * @param onSelectedChange makes the row selectable, called with the next selected state.
 * @param swipeActions read, archive and delete, offered by swiping and as TalkBack actions.
 * @param labels replacements for the built-in strings.
 * @param interactionSource the interaction source of the row.
 * @param badges markers on the last line: status, SLA, priority.
 * @param trailing the end of the last line: a reference number or the assignee's avatar.
 */
@Composable
public fun MonarchInboxRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    unread: Boolean = false,
    unreadCount: Int? = null,
    selected: Boolean = false,
    current: Boolean = false,
    selecting: Boolean = false,
    preview: String? = null,
    timestamp: Long? = null,
    now: Long? = null,
    avatarName: String? = null,
    avatarImage: Painter? = null,
    showAvatar: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
    iconLabel: String? = null,
    onSelectedChange: ((Boolean) -> Unit)? = null,
    swipeActions: List<MonarchInboxRowSwipeAction> = emptyList(),
    labels: MonarchInboxRowLabels = MonarchInboxRowLabels(),
    interactionSource: MutableInteractionSource? = null,
    badges: (@Composable RowScope.() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val tokens = MonarchInboxRowTokens
    val isUnread = MonarchInboxRowLogic.isUnread(unread, unreadCount)
    val actionNames = swipeActions.map { actionLabel(it, isUnread, labels) }
    val rowModifier = Modifier.inboxRowInteraction(
        onClick = onClick,
        selection = InboxRowSelection(
            selected = selected,
            selecting = selecting,
            onSelectedChange = onSelectedChange,
            label = monarchString(labels.select, R.string.monarch_inboxrow_select),
        ),
        actions = swipeActions,
        actionNames = actionNames,
        interactionSource = interactionSource,
    )
    val item: @Composable (Modifier) -> Unit = { itemModifier ->
        // The row keeps its own tap, long press and selected state; the opaque fill covers the swipe background.
        MonarchListRow(
            headlineContent = {
                HeadLine(title, isUnread, unreadCount, timestamp, now, icon, iconLabel, labels)
            },
            modifier = itemModifier.then(rowModifier)
                .background(
                    if (current && !selected) {
                        MonarchTheme.colors.accentSubtle
                    } else {
                        tokens.container(selected, MonarchTheme.colors)
                    },
                )
                .then(if (current) Modifier.currentBar(MonarchTheme.colors.accentSolid) else Modifier),
            supportingContent = if (preview != null || badges != null || trailing != null) {
                { Supporting(preview, badges, trailing, indented = isUnread && unreadCount?.takeIf { it > 0 } == null) }
            } else {
                null
            },
            leadingContent = leading(
                checkbox = onSelectedChange != null && (selecting || selected),
                selected = selected,
                showAvatar = showAvatar,
                avatarImage = avatarImage,
                avatarName = avatarName ?: title,
            ),
        )
    }
    if (swipeActions.isEmpty()) {
        item(modifier)
    } else {
        SwipeRow(modifier, swipeActions, actionNames, item)
    }
}

/** The selection state of a row and the label of its long press. */
private class InboxRowSelection(
    val selected: Boolean,
    val selecting: Boolean,
    val onSelectedChange: ((Boolean) -> Unit)?,
    val label: String,
)

/** The tap, the long press, the selected state and the TalkBack actions of a row. */
private fun Modifier.inboxRowInteraction(
    onClick: () -> Unit,
    selection: InboxRowSelection,
    actions: List<MonarchInboxRowSwipeAction>,
    actionNames: List<String>,
    interactionSource: MutableInteractionSource?,
): Modifier {
    val selected = selection.selected
    val onSelectedChange = selection.onSelectedChange
    val toggle = onSelectedChange?.let { change -> { change(!selected) } }
    val selectLabel = selection.label
    val selecting = selection.selecting
    return this
        .combinedClickable(
            interactionSource = interactionSource,
            indication = ripple(),
            onLongClickLabel = selectLabel.takeIf { toggle != null },
            onLongClick = toggle,
            onClick = if (selecting && toggle != null) toggle else onClick,
        )
        .semantics {
            if (onSelectedChange != null) this.selected = selected
            if (actions.isNotEmpty()) {
                customActions = actions.mapIndexed { index, action ->
                    CustomAccessibilityAction(actionNames[index]) {
                        action.onAction()
                        true
                    }
                }
            }
        }
}

/** A checkbox while selecting, the avatar, or nothing. */
private fun leading(
    checkbox: Boolean,
    selected: Boolean,
    showAvatar: Boolean,
    avatarImage: Painter?,
    avatarName: String,
): (@Composable () -> Unit)? = when {
    checkbox -> {
        {
            MonarchCheckbox(checked = selected, onCheckedChange = null, modifier = Modifier.clearAndSetSemantics { })
        }
    }
    showAvatar -> {
        { MonarchAvatar(image = avatarImage, name = avatarName, size = MonarchAvatarSize.Md, decorative = true) }
    }
    else -> null
}

@Composable
private fun SwipeRow(
    modifier: Modifier,
    actions: List<MonarchInboxRowSwipeAction>,
    names: List<String>,
    item: @Composable (Modifier) -> Unit,
) {
    val tokens = MonarchInboxRowTokens
    val towardEnd = MonarchInboxRowLogic.towardEnd(actions)
    val towardStart = MonarchInboxRowLogic.towardStart(actions)
    val state = rememberSwipeToDismissBoxState()
    val scope = rememberCoroutineScope()
    SwipeToDismissBox(
        state = state,
        backgroundContent = {
            val action = when (state.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> towardEnd
                SwipeToDismissBoxValue.EndToStart -> towardStart
                SwipeToDismissBoxValue.Settled -> null
            }
            if (action != null) {
                val colors = tokens.swipeColors(action, MonarchTheme.colors)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.container)
                        .padding(horizontal = tokens.SwipePadding),
                    contentAlignment = if (action === towardEnd) Alignment.CenterStart else Alignment.CenterEnd,
                ) {
                    Text(
                        text = names[actions.indexOf(action)],
                        style = MonarchTheme.typography.label,
                        color = colors.content,
                    )
                }
            }
        },
        modifier = modifier,
        enableDismissFromStartToEnd = towardEnd != null,
        enableDismissFromEndToStart = towardStart != null,
        onDismiss = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> towardEnd?.onAction?.invoke()
                SwipeToDismissBoxValue.EndToStart -> towardStart?.onAction?.invoke()
                SwipeToDismissBoxValue.Settled -> Unit
            }
            scope.launch { state.reset() }
        },
    ) {
        item(Modifier)
    }
}

@Composable
private fun HeadLine(
    title: String,
    unread: Boolean,
    unreadCount: Int?,
    timestamp: Long?,
    now: Long?,
    icon: (@Composable () -> Unit)?,
    iconLabel: String?,
    labels: MonarchInboxRowLabels,
) {
    val tokens = MonarchInboxRowTokens
    val colors = MonarchTheme.colors
    val text = tokens.textColors(unread, colors)
    val mark = tokens.markColors(colors)
    val count = unreadCount?.takeIf { it > 0 }
    val unreadText = if (!unread) {
        null
    } else {
        labels.unread?.invoke(count) ?: if (count == null) {
            stringResource(R.string.monarch_inboxrow_unread)
        } else {
            pluralStringResource(R.plurals.monarch_inboxrow_unread_count, count, count)
        }
    }
    val spokenTitle = listOfNotNull(title, unreadText, iconLabel?.takeIf { it.isNotBlank() })
        .joinToString(stringResource(R.string.monarch_inboxrow_separator))
    Row(horizontalArrangement = Arrangement.spacedBy(tokens.Gap), verticalAlignment = Alignment.CenterVertically) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (unread && count == null) {
                Box(modifier = Modifier.size(tokens.DotSize).clip(MonarchShapeDefaults.Mark).background(mark.container))
            }
            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .clearAndSetSemantics { contentDescription = spokenTitle },
            ) {
                Text(
                    text = title,
                    style = monarchFieldTextStyle().copy(fontWeight = tokens.titleWeight(unread)),
                    color = text.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (icon != null) {
                Box(modifier = Modifier.clearAndSetSemantics { }) {
                    CompositionLocalProvider(LocalContentColor provides colors.textMuted) { icon() }
                }
            }
        }
        if (timestamp != null) Age(timestamp, now, unread, labels)
        if (count != null) {
            Box(
                modifier = Modifier
                    .clip(MonarchShapeDefaults.Mark)
                    .background(mark.container)
                    .padding(horizontal = tokens.CountPadding)
                    .clearAndSetSemantics { },
            ) {
                Text(
                    text = MonarchInboxRowLogic.countText(count),
                    style = MonarchTheme.typography.caption,
                    color = mark.content,
                )
            }
        }
    }
}

/** Whether ages read in full words ('18 minutes ago') as in the notification centre, not compact ('18m'). */
internal val LocalMonarchInboxRowLongAge = androidx.compose.runtime.compositionLocalOf { false }

@Composable
private fun Age(timestamp: Long, now: Long?, unread: Boolean, labels: MonarchInboxRowLabels) {
    val tokens = MonarchInboxRowTokens
    val clock = rememberMonarchClock(now)
    val locale = LocalConfiguration.current.locales[0]
    val age = MonarchInboxRowLogic.compactAge(timestamp, clock)
    val visible = when {
        LocalMonarchInboxRowLongAge.current && age != null ->
            com.rookhq.monarch.internal.MonarchRelativeTime.format(timestamp, clock, locale, future = false)
        age == null -> MonarchInboxRowLogic.shortDate(timestamp, clock, locale)
        labels.age != null -> labels.age.invoke(age.unit, age.value)
        else -> stringResource(ageResource(age.unit), age.value)
    }
    val full = MonarchInboxRowLogic.fullDate(timestamp, locale)
    val color = tokens.textColors(unread, MonarchTheme.colors).age
    Box(modifier = Modifier.clearAndSetSemantics { contentDescription = full }) {
        Text(
            text = visible,
            style = MonarchTheme.typography.caption.copy(fontWeight = tokens.titleWeight(unread)),
            color = color,
            maxLines = 1,
        )
    }
}

@Composable
private fun Supporting(
    preview: String?,
    badges: (@Composable RowScope.() -> Unit)?,
    trailing: (@Composable () -> Unit)?,
    indented: Boolean,
) {
    val tokens = MonarchInboxRowTokens
    // With the unread dot the title text starts after it; the preview starts on the same edge as the title text.
    val start = if (indented) tokens.DotSize + tokens.Gap else MonarchTokens.Space.space0.dp
    Column(Modifier.padding(start = start), verticalArrangement = Arrangement.spacedBy(tokens.Gap)) {
        if (preview != null) {
            Text(
                text = preview,
                style = monarchFieldTextStyle(),
                color = tokens.textColors(false, MonarchTheme.colors).preview,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (badges != null || trailing != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
                    verticalAlignment = Alignment.CenterVertically,
                ) { badges?.invoke(this) }
                trailing?.invoke()
            }
        }
    }
}

@Composable
private fun actionLabel(action: MonarchInboxRowSwipeAction, unread: Boolean, labels: MonarchInboxRowLabels): String =
    when (action) {
        is MonarchInboxRowSwipeAction.ToggleRead -> if (unread) {
            monarchString(labels.markRead, R.string.monarch_inboxrow_mark_read)
        } else {
            monarchString(labels.markUnread, R.string.monarch_inboxrow_mark_unread)
        }
        is MonarchInboxRowSwipeAction.Archive -> monarchString(labels.archive, R.string.monarch_inboxrow_archive)
        is MonarchInboxRowSwipeAction.Delete -> monarchString(labels.delete, R.string.monarch_inboxrow_delete)
    }

private fun ageResource(unit: MonarchInboxRowAgeUnit): Int = when (unit) {
    MonarchInboxRowAgeUnit.Now -> R.string.monarch_inboxrow_age_now
    MonarchInboxRowAgeUnit.Minutes -> R.string.monarch_inboxrow_age_minutes
    MonarchInboxRowAgeUnit.Hours -> R.string.monarch_inboxrow_age_hours
    MonarchInboxRowAgeUnit.Days -> R.string.monarch_inboxrow_age_days
    MonarchInboxRowAgeUnit.Weeks -> R.string.monarch_inboxrow_age_weeks
}

/** The bar at the start of the row that is open beside the list. */
private fun Modifier.currentBar(color: androidx.compose.ui.graphics.Color): Modifier = drawBehind {
    val width = MonarchInboxRowTokens.CurrentBarWidth.toPx()
    drawRect(color, size = androidx.compose.ui.geometry.Size(width, size.height))
}
