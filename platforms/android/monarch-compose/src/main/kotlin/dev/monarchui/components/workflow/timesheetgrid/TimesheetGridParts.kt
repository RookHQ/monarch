package dev.monarchui.components.workflow.timesheetgrid

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import dev.monarchui.accessibility.monarchFocusRing
import dev.monarchui.accessibility.monarchMinimumTouchTarget
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.actions.iconbutton.MonarchIconButton
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.foundation.icon.MonarchIconSize
import dev.monarchui.internal.MonarchGlyphs
import dev.monarchui.components.overlays.menu.MonarchMenu
import dev.monarchui.components.overlays.menu.MonarchMenuItem
import dev.monarchui.components.typography.tabularnumberstyle.MonarchTabularNumberStyle
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.theme.MonarchTheme

/**
 * A day's heading: "Today", the short (or [long]) day, the lock reason and the note, read as one heading with the full
 * date. A button when the grid has `onDayActivate`. Shaded when the day is locked or not a working day; today
 * has an accent bar under it.
 */
@Composable
internal fun TimesheetGridDayHeading(
    model: TimesheetGridModel,
    day: MonarchTimesheetGridDay,
    modifier: Modifier = Modifier,
    long: Boolean = false,
) {
    val tokens = MonarchTimesheetGridTokens
    val colors = MonarchTheme.colors
    val color = tokens.dayColor(colors, day.today)
    val name = day.label ?: if (long) model.text.longDay(day.date) else model.text.shortDay(day.date)
    val bar = tokens.todayBar(colors)
    val activate = model.onDayActivate
    Column(
        modifier = modifier
            .background(tokens.columnContainer(colors, TimesheetGridRules.locked(day), day.nonWorking))
            .then(
                if (day.today) {
                    Modifier.drawBehind {
                        val height = tokens.TodayBarHeight.toPx()
                        drawRect(bar, topLeft = Offset(0f, size.height - height), size = Size(size.width, height))
                    }
                } else {
                    Modifier
                },
            )
            .then(
                if (activate != null) {
                    Modifier
                        .monarchMinimumTouchTarget()
                        .monarchFocusRing(MonarchShapeDefaults.Row)
                        .clickable(role = Role.Button) { activate(day.date) }
                } else {
                    Modifier
                },
            )
            .padding(tokens.HeaderPadding)
            .semantics(mergeDescendants = true) {
                heading()
                contentDescription = model.spokenDay(day)
            },
        verticalArrangement = Arrangement.spacedBy(tokens.LineGap),
    ) {
        Text(name, style = MonarchTheme.typography.label, color = color)
        model.lockText(day)?.let { lock ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(tokens.LineGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MonarchIcon(
                    MonarchGlyphs.Lock,
                    contentDescription = null,
                    size = MonarchIconSize.Sm,
                    tint = tokens.muted(colors),
                )
                Text(lock, style = MonarchTheme.typography.caption, color = tokens.muted(colors))
            }
        }
        day.note?.let { Text(it, style = MonarchTheme.typography.caption, color = tokens.muted(colors)) }
    }
}

/** A row's name and description as one heading, and its actions menu. */
@Composable
internal fun TimesheetGridRowHeader(model: TimesheetGridModel, row: MonarchTimesheetGridRow, modifier: Modifier) {
    val tokens = MonarchTimesheetGridTokens
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(tokens.CellPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f).semantics(mergeDescendants = true) { heading() },
            verticalArrangement = Arrangement.spacedBy(tokens.LineGap),
        ) {
            Text(row.label, style = MonarchTheme.typography.bodyStrong, color = MonarchTheme.colors.textDefault)
            row.description?.let {
                Text(it, style = MonarchTheme.typography.caption, color = tokens.muted(MonarchTheme.colors))
            }
        }
        TimesheetGridRowActions(model, row)
    }
}

/** A cell of the grid: a field when it takes typing, a button when it opens its entries, else text. */
@Composable
internal fun TimesheetGridCell(model: TimesheetGridModel, row: MonarchTimesheetGridRow, day: MonarchTimesheetGridDay) {
    val saved = row.minutes(day.date)
    val activate = model.onCellActivate
    when {
        TimesheetGridRules.cellEditable(model.editable, row, day) -> TimesheetGridField(model, row, day)
        activate != null && saved > 0 -> TimesheetGridValueButton(model, row, day) { activate(row.id, day.date) }
        else -> TimesheetGridValue(model, row, day, Modifier)
    }
}

/** A read-only value: the time, or a dash, read as the cell's name with the time in words and the lock reason. */
@Composable
internal fun TimesheetGridValue(
    model: TimesheetGridModel,
    row: MonarchTimesheetGridRow,
    day: MonarchTimesheetGridDay,
    modifier: Modifier,
) {
    val saved = row.minutes(day.date)
    Text(
        text = model.shown(saved),
        modifier = modifier.clearAndSetSemantics {
            contentDescription = model.cellLabel(row, day)
            stateDescription = model.spokenCell(saved, day)
        },
        style = MonarchTabularNumberStyle.applyTo(MonarchTheme.typography.body),
        color = MonarchTimesheetGridTokens.valueColor(MonarchTheme.colors, TimesheetGridRules.locked(day)),
        textAlign = TextAlign.End,
    )
}

/** A value that opens the entries behind it: a small link `MonarchButton`. */
@Composable
internal fun TimesheetGridValueButton(
    model: TimesheetGridModel,
    row: MonarchTimesheetGridRow,
    day: MonarchTimesheetGridDay,
    onClick: () -> Unit,
) {
    val saved = row.minutes(day.date)
    MonarchButton(
        onClick = onClick,
        modifier = Modifier.semantics {
            contentDescription = model.cellLabel(row, day)
            stateDescription = model.spokenCell(saved, day)
        },
        variant = MonarchButtonVariant.Link,
        size = MonarchButtonSize.Sm,
    ) {
        Text(
            text = model.shown(saved),
            modifier = Modifier.clearAndSetSemantics { },
            style = MonarchTabularNumberStyle.applyTo(MonarchTheme.typography.body),
        )
    }
}

/** A day total: the time, the expected time and "Over limit", read as one element named by the day. */
@Composable
internal fun TimesheetGridDayTotal(model: TimesheetGridModel, day: MonarchTimesheetGridDay, modifier: Modifier) {
    val tokens = MonarchTimesheetGridTokens
    val colors = MonarchTheme.colors
    val total = TimesheetGridRules.dayTotal(model.rows, day)
    val over = TimesheetGridRules.over(total, model.maximum)
    Column(
        modifier = modifier.clearAndSetSemantics {
            contentDescription = model.text.cellLabel(model.text.dayTotal, model.text.longDay(day.date))
            stateDescription = model.spokenDayTotal(total, day)
        },
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(tokens.LineGap),
    ) {
        Text(
            text = model.shown(total),
            style = MonarchTabularNumberStyle.applyTo(MonarchTheme.typography.bodyStrong),
            color = tokens.totalColor(colors, over),
        )
        day.expectedMinutes?.let {
            Text(
                text = model.text.expected(model.text.duration(it)),
                style = MonarchTheme.typography.caption,
                color = tokens.muted(colors),
            )
        }
        if (over) {
            Text(text = model.text.overLimit, style = MonarchTheme.typography.caption, color = tokens.error(colors))
        }
    }
}

/** A row's actions: a button that opens a `MonarchMenu`. Nothing when the row has no actions. */
@Composable
internal fun TimesheetGridRowActions(model: TimesheetGridModel, row: MonarchTimesheetGridRow) {
    val actions = model.rowActions?.invoke(row).orEmpty()
    if (actions.isEmpty()) return
    var open by remember { mutableStateOf(false) }
    val name = model.text.rowActions(row.label)
    val icon = model.actionsIcon
    Box {
        if (icon != null) {
            MonarchIconButton(label = name, onClick = { open = true }, icon = icon)
        } else {
            MonarchButton(
                text = model.text.actionsText,
                onClick = { open = true },
                modifier = Modifier.semantics { contentDescription = name },
                variant = MonarchButtonVariant.Ghost,
                size = MonarchButtonSize.Sm,
            )
        }
        MonarchMenu(expanded = open, onDismissRequest = { open = false }) {
            actions.forEach { action ->
                MonarchMenuItem(
                    text = action.title,
                    onClick = {
                        open = false
                        action.onClick()
                    },
                    enabled = action.enabled,
                    destructive = action.danger,
                )
            }
        }
    }
}
