package com.rookhq.monarch.components.workflow.issuecard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.R
import com.rookhq.monarch.accessibility.MonarchTouchTarget
import com.rookhq.monarch.accessibility.monarchFocusRing
import com.rookhq.monarch.components.datadisplay.card.MonarchCard
import com.rookhq.monarch.components.datadisplay.tag.MonarchTag
import com.rookhq.monarch.components.datadisplay.tag.MonarchTagSize
import com.rookhq.monarch.components.identity.avatar.MonarchAvatar
import com.rookhq.monarch.components.identity.avatar.MonarchAvatarSize
import com.rookhq.monarch.components.identity.avatar.MonarchAvatarTokens
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.components.workflow.prioritypicker.MonarchPriorityGlyph
import com.rookhq.monarch.components.workflow.prioritypicker.MonarchPriorityLevel
import com.rookhq.monarch.components.workflow.prioritypicker.MonarchPriorityPicker
import com.rookhq.monarch.components.workflow.prioritypicker.MonarchPriorityPickerLabels
import com.rookhq.monarch.components.workflow.prioritypicker.MonarchPriorityPickerSize
import com.rookhq.monarch.components.workflow.statuspicker.MonarchStatusGlyph
import com.rookhq.monarch.components.workflow.statuspicker.MonarchStatusOption
import com.rookhq.monarch.components.workflow.statuspicker.MonarchStatusPicker
import com.rookhq.monarch.components.workflow.statuspicker.MonarchStatusPickerSize
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.foundation.monarchShadow
import com.rookhq.monarch.internal.MonarchDefaultIcons
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme
import java.util.TimeZone

/**
 * An issue on a board or in a list: key, title, status, priority, tags, due
 * date and assignee, with status and priority changeable in place.
 *
 * A Monarch pattern built from a selectable
 * `MonarchCard`, `MonarchStatusPicker`, `MonarchPriorityPicker`, `MonarchTag`
 * and `MonarchAvatar`. The title is the one control that opens the issue and
 * is read with the key first ("STR-142, Sign-in fails"). Every priority
 * level and status category has its own shape, and overdue is written, so
 * nothing depends on colour alone.
 *
 * @param title the issue's title, already localised.
 * @param key the issue key, such as STR-142. Read before the title.
 * @param modifier the modifier of the card.
 * @param status the current status, shown with the glyph of its category. Completed and cancelled issues are never
 * overdue.
 * @param statuses every status. With [onStatusChange], the status becomes a `MonarchStatusPicker`.
 * @param priority the priority, shown at the end of the header. None when `null`.
 * @param assignee a person shows an avatar; [MonarchIssueCardAssignee.Unassigned] an empty dashed slot. No slot when
 * `null`.
 * @param tags labels on the issue, shown as small tags.
 * @param dueDate the due day in epoch milliseconds, read in UTC as a Material 3 `DatePickerState` stores it.
 * @param today the reference day for overdue, in the same form. The device's current day when `null`.
 * @param selected tints the card with the accent and thickens its border; TalkBack reads the title as selected.
 * @param dragging lifts the card with the presented shadow, for a card being dragged on a board. At rest the
 * card has no shadow.
 * @param labels replacements for the built-in strings.
 * @param onStatusChange changes the status from the card. Receives the chosen status id.
 * @param onPriorityChange changes the priority from the card; the glyph becomes a `MonarchPriorityPicker`.
 * @param onOpen opens the issue. Makes the title a button.
 */
@Suppress("LongParameterList", "LongMethod")
@ExperimentalMonarchApi
@Composable
public fun MonarchIssueCard(
    title: String,
    key: String,
    modifier: Modifier = Modifier,
    status: MonarchStatusOption? = null,
    statuses: List<MonarchStatusOption> = emptyList(),
    priority: MonarchPriorityLevel? = null,
    assignee: MonarchIssueCardAssignee? = null,
    tags: List<String> = emptyList(),
    dueDate: Long? = null,
    today: Long? = null,
    selected: Boolean = false,
    dragging: Boolean = false,
    labels: MonarchIssueCardLabels = MonarchIssueCardLabels(),
    onStatusChange: ((String) -> Unit)? = null,
    onPriorityChange: ((MonarchPriorityLevel) -> Unit)? = null,
    onOpen: (() -> Unit)? = null,
) {
    val shape = MonarchTheme.shapes.lg
    MonarchCard(
        // An inline card has no shadow at rest; only a dragged card lifts, with the one presented shadow.
        modifier = if (dragging) modifier.monarchShadow(MonarchTheme.elevation.presented, shape) else modifier,
        contentPadding = MonarchIssueCardTokens.ContentPadding,
        fillWidth = false,
        selected = selected,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(MonarchIssueCardTokens.Gap)) {
            Header(key, priority, labels, onPriorityChange)
            Title(title, key, selected, onOpen)
            Row(verticalAlignment = Alignment.CenterVertically) {
                FlowRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(MonarchIssueCardTokens.MetaGap),
                    verticalArrangement = Arrangement.spacedBy(MonarchIssueCardTokens.InlineGap),
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    status?.let { StatusPart(it, statuses, labels, onStatusChange) }
                    if (tags.isNotEmpty()) TagsPart(tags, labels)
                    dueDate?.let { DuePart(it, today, status, labels) }
                }
                assignee?.let { AssigneePart(it, labels) }
            }
        }
    }
}

@Composable
private fun Header(
    key: String,
    priority: MonarchPriorityLevel?,
    labels: MonarchIssueCardLabels,
    onPriorityChange: ((MonarchPriorityLevel) -> Unit)?,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        // The key is read with the title, so it is hidden here.
        MonarchText(
            text = key,
            modifier = Modifier.weight(1f).clearAndSetSemantics {},
            style = MonarchTextStyle.Caption,
            color = MonarchTheme.colors.textMuted,
        )
        if (priority == null) return@Row
        val levelName = labels.priorityLevel?.invoke(priority) ?: priorityName(priority)
        if (onPriorityChange != null) {
            MonarchPriorityPicker(
                value = priority,
                onValueChange = onPriorityChange,
                size = MonarchPriorityPickerSize.Sm,
                showLabel = false,
                labels = priorityPickerLabels(labels),
            )
        } else {
            val description = labels.priority?.invoke(levelName)
                ?: stringResource(R.string.monarch_issuecard_priority, levelName)
            Box(
                Modifier.semantics {
                    contentDescription = description
                    role = Role.Image
                },
            ) { MonarchPriorityGlyph(priority) }
        }
    }
}

@Composable
private fun Title(title: String, key: String, selected: Boolean, onOpen: (() -> Unit)?) {
    val described = Modifier.semantics(mergeDescendants = true) {
        contentDescription = "$key, $title"
        if (selected) this.selected = true
    }
    val base = if (onOpen != null) {
        Modifier
            .fillMaxWidth()
            .heightIn(min = MonarchTouchTarget.Minimum)
            .monarchFocusRing(MonarchShapeDefaults.Row)
            .clickable(role = Role.Button, interactionSource = null, indication = ripple(), onClick = onOpen)
    } else {
        Modifier.fillMaxWidth()
    }
    Box(modifier = base.then(described), contentAlignment = Alignment.CenterStart) {
        MonarchText(text = title, style = MonarchTextStyle.BodyStrong)
    }
}

@Composable
private fun StatusPart(
    status: MonarchStatusOption,
    statuses: List<MonarchStatusOption>,
    labels: MonarchIssueCardLabels,
    onStatusChange: ((String) -> Unit)?,
) {
    if (onStatusChange != null && statuses.isNotEmpty()) {
        MonarchStatusPicker(
            statuses = statuses,
            value = status.id,
            onValueChange = onStatusChange,
            size = MonarchStatusPickerSize.Sm,
        )
    } else {
        val description = labels.status?.invoke(status.name)
            ?: stringResource(R.string.monarch_issuecard_status, status.name)
        Row(
            modifier = Modifier.clearAndSetSemantics { contentDescription = description },
            horizontalArrangement = Arrangement.spacedBy(MonarchIssueCardTokens.InlineGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MonarchStatusGlyph(status.category)
            MonarchText(text = status.name, style = MonarchTextStyle.Caption, color = MonarchTheme.colors.textMuted)
        }
    }
}

@Composable
private fun TagsPart(tags: List<String>, labels: MonarchIssueCardLabels) {
    val joined = tags.joinToString(", ")
    val description = labels.tags?.invoke(joined) ?: stringResource(R.string.monarch_issuecard_tags, joined)
    FlowRow(
        modifier = Modifier.clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(MonarchIssueCardTokens.InlineGap),
        verticalArrangement = Arrangement.spacedBy(MonarchIssueCardTokens.InlineGap),
    ) {
        tags.forEach { MonarchTag(text = it, size = MonarchTagSize.Sm) }
    }
}

@Composable
private fun DuePart(dueDate: Long, today: Long?, status: MonarchStatusOption?, labels: MonarchIssueCardLabels) {
    val locale = LocalConfiguration.current.locales[0]
    val day = remember(dueDate, locale) { MonarchIssueCardTokens.formatDue(dueDate, locale) }
    val todayDay = today?.let(MonarchIssueCardTokens::day)
        ?: MonarchIssueCardTokens.today(System.currentTimeMillis(), TimeZone.getDefault())
    val overdue = MonarchIssueCardTokens.isOverdue(dueDate, todayDay, status)
    val dueText = labels.due?.invoke(day) ?: stringResource(R.string.monarch_issuecard_due, day)
    val overdueText = monarchString(labels.overdue, R.string.monarch_issuecard_overdue)
    val color = MonarchIssueCardTokens.dueColor(overdue, MonarchTheme.colors)
    Row(
        modifier = Modifier.clearAndSetSemantics {
            contentDescription = if (overdue) "$dueText, $overdueText" else dueText
        },
        horizontalArrangement = Arrangement.spacedBy(MonarchIssueCardTokens.InlineGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalContentColor provides color) { MonarchDefaultIcons.Calendar() }
        MonarchText(text = dueText, style = MonarchTextStyle.Caption, color = color)
        if (overdue) MonarchText(text = overdueText, style = MonarchTextStyle.Label, color = color)
    }
}

@Composable
private fun AssigneePart(assignee: MonarchIssueCardAssignee, labels: MonarchIssueCardLabels) {
    when (assignee) {
        is MonarchIssueCardAssignee.Person -> {
            val description = labels.assignee?.invoke(assignee.name)
                ?: stringResource(R.string.monarch_issuecard_assignee, assignee.name)
            Box(
                Modifier.semantics {
                    contentDescription = description
                    role = Role.Image
                },
            ) {
                MonarchAvatar(
                    image = assignee.image,
                    name = assignee.name,
                    size = MonarchAvatarSize.Sm,
                    decorative = true,
                )
            }
        }
        MonarchIssueCardAssignee.Unassigned -> {
            val description = monarchString(labels.unassigned, R.string.monarch_issuecard_unassigned)
            val line = MonarchIssueCardTokens.unassignedColor(MonarchTheme.colors)
            Box(
                Modifier
                    .size(MonarchAvatarTokens.side(MonarchAvatarSize.Sm))
                    .semantics {
                        contentDescription = description
                        role = Role.Image
                    }
                    .drawBehind {
                        val width = MonarchIssueCardTokens.DashedBorderWidth.toPx()
                        drawCircle(
                            color = line,
                            radius = (size.minDimension - width) / 2,
                            style = Stroke(
                                width = width,
                                pathEffect = PathEffect.dashPathEffect(
                                    floatArrayOf(
                                        MonarchIssueCardTokens.DASH * width,
                                        MonarchIssueCardTokens.DASH_GAP * width,
                                    ),
                                ),
                            ),
                        )
                    },
            )
        }
    }
}

@Composable
private fun priorityName(level: MonarchPriorityLevel): String = stringResource(
    when (level) {
        MonarchPriorityLevel.Urgent -> R.string.monarch_prioritypicker_urgent
        MonarchPriorityLevel.High -> R.string.monarch_prioritypicker_high
        MonarchPriorityLevel.Medium -> R.string.monarch_prioritypicker_medium
        MonarchPriorityLevel.Low -> R.string.monarch_prioritypicker_low
        MonarchPriorityLevel.None -> R.string.monarch_prioritypicker_none
    },
)

private fun priorityPickerLabels(labels: MonarchIssueCardLabels): MonarchPriorityPickerLabels {
    val name = labels.priorityLevel ?: return MonarchPriorityPickerLabels(trigger = labels.priority)
    return MonarchPriorityPickerLabels(
        trigger = labels.priority,
        urgent = name(MonarchPriorityLevel.Urgent),
        high = name(MonarchPriorityLevel.High),
        medium = name(MonarchPriorityLevel.Medium),
        low = name(MonarchPriorityLevel.Low),
        none = name(MonarchPriorityLevel.None),
    )
}
