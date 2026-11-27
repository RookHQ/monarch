package com.rookhq.monarch.components.workflow.timesheetgrid

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.rookhq.monarch.components.forms.durationinput.MonarchDurationInput
import com.rookhq.monarch.components.forms.durationinput.MonarchDurationInputLabels
import com.rookhq.monarch.components.layout.divider.MonarchDivider
import com.rookhq.monarch.components.navigation.tabs.MonarchTabs
import com.rookhq.monarch.theme.MonarchTheme

/**
 * One day at a time: `MonarchTabs` of the days, the day's heading, a row per timesheet
 * row (a `MonarchDurationInput` when it takes typing), the day total and the summary rows. Opens on today.
 */
@Composable
internal fun TimesheetGridDayPages(model: TimesheetGridModel) {
    val days = model.days
    if (days.isEmpty()) {
        Text(model.emptyMessage, style = MonarchTheme.typography.body, color = MonarchTheme.colors.textMuted)
        return
    }
    var selected by rememberSaveable(days.first().date, days.size) {
        mutableIntStateOf(TimesheetGridRules.initialDayIndex(days))
    }
    val index = selected.coerceIn(0, days.lastIndex)
    val day = days[index]
    Column(verticalArrangement = Arrangement.spacedBy(MonarchTimesheetGridTokens.Gap)) {

        MonarchTabs(
            selectedIndex = index,
            onSelectedIndexChange = { selected = it },
            modifier = Modifier.semantics { contentDescription = model.text.days },
        ) {
            days.forEach { tabDay ->
                tab(
                    text = tabDay.label ?: model.text.shortDay(tabDay.date),
                    contentDescription = model.spokenDay(tabDay),
                    secondary = model.text.today.takeIf { tabDay.today },
                    accent = tabDay.today,
                )
            }
        }
        TimesheetGridDayHeading(model, day, Modifier.fillMaxWidth(), long = true)
        if (model.rows.isEmpty()) {
            Text(model.emptyMessage, style = MonarchTheme.typography.body, color = MonarchTheme.colors.textMuted)
        } else {
            Column {
                model.rows.forEachIndexed { i, row ->
                    if (i > 0) MonarchDivider()
                    TimesheetGridDayRow(model, row, day)
                }
            }
        }
        MonarchDivider()
        TimesheetGridDayFooter(model, day)
    }
}

/** One timesheet row on the day page: a duration field, or the name with the value. */
@Composable
private fun TimesheetGridDayRow(model: TimesheetGridModel, row: MonarchTimesheetGridRow, day: MonarchTimesheetGridDay) {
    val tokens = MonarchTimesheetGridTokens
    val saved = row.minutes(day.date)
    val edit = model.editor.state(model.key(row, day), saved)
    val total = model.text.periodTotal(model.text.duration(TimesheetGridRules.rowTotal(row, model.days)))
    val activate = model.onCellActivate
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = tokens.RowPadding),
        horizontalArrangement = Arrangement.spacedBy(tokens.RowGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (TimesheetGridRules.cellEditable(model.editable, row, day)) {
            val text = model.text
            MonarchDurationInput(
                minutes = saved.takeIf { it > 0 },
                onMinutesChange = { model.save(row, day, it ?: 0) },
                modifier = Modifier.weight(1f),
                label = row.label,
                helperText = if (edit.busy) text.saving else listOfNotNull(row.description, total).joinToString(" · "),
                errorText = edit.error,
                maximum = model.maximum,
                enabled = !edit.busy,
                format = text.duration,
                labels = MonarchDurationInputLabels(
                    invalid = text.invalid,
                    range = { _, maximum -> text.tooLong(text.duration(maximum ?: model.maximum)) },
                    valueText = text.spokenDuration,
                ),
            )
        } else {
            Column(
                modifier = Modifier.weight(1f).semantics(mergeDescendants = true) { },
                verticalArrangement = Arrangement.spacedBy(tokens.LineGap),
            ) {
                Text(row.label, style = MonarchTheme.typography.bodyStrong, color = MonarchTheme.colors.textDefault)
                row.description?.let {
                    Text(it, style = MonarchTheme.typography.caption, color = tokens.muted(MonarchTheme.colors))
                }
                Text(total, style = MonarchTheme.typography.caption, color = tokens.muted(MonarchTheme.colors))
            }
            if (activate != null && saved > 0) {
                TimesheetGridValueButton(model, row, day) { activate(row.id, day.date) }
            } else {
                TimesheetGridValue(model, row, day, Modifier)
            }
        }
        TimesheetGridRowActions(model, row)
    }
}

/** The day total, then each summary row: a name at the start, the value at the end. */
@Composable
private fun TimesheetGridDayFooter(model: TimesheetGridModel, day: MonarchTimesheetGridDay) {
    val tokens = MonarchTimesheetGridTokens
    Column(verticalArrangement = Arrangement.spacedBy(tokens.CellPadding)) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(tokens.RowGap)) {
            Text(
                text = model.text.dayTotal,
                modifier = Modifier.weight(1f).clearAndSetSemantics { },
                style = MonarchTheme.typography.label,
                color = MonarchTheme.colors.textDefault,
            )
            TimesheetGridDayTotal(model, day, Modifier)
        }
        model.summaryRows.forEach { summary ->
            val value = summary.text(day.date)
            Row(
                modifier = Modifier.clearAndSetSemantics {
                    contentDescription = summary.label
                    stateDescription = value ?: model.text.noTime
                },
                horizontalArrangement = Arrangement.spacedBy(tokens.RowGap),
            ) {
                Text(
                    text = summary.label,
                    modifier = Modifier.weight(1f),
                    style = MonarchTheme.typography.caption,
                    color = tokens.muted(MonarchTheme.colors),
                )
                Text(
                    text = value ?: tokens.EMPTY_MARK,
                    style = MonarchTheme.typography.caption,
                    color = tokens.muted(MonarchTheme.colors),
                )
            }
        }
    }
}
