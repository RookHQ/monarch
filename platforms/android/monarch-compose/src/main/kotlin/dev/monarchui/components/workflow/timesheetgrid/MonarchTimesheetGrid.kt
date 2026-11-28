package dev.monarchui.components.workflow.timesheetgrid

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.datagrid.datatable.MonarchDataTableRowAction
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.theme.MonarchTheme

/**
 * A period of time entry, usually a week: rows of projects or tasks against days, with typed durations ("1:30",
 * "1.5", "90m", "1h 30m"), row, day and period totals, locked days and a daily limit. Data in, changes out: the
 * app saves each cell in [onCellChange] and passes the new [rows].
 *
 * Built by Monarch, because Material 3 has no time grid. A grid of rows by days does not fit a phone, so the
 * [Automatic][MonarchTimesheetGridLayout.Automatic] layout shows the grid only when every day column fits the
 * width and the text is not enlarged much (a tablet); otherwise it shows one day at a time, picked with a
 * Material 3 `PrimaryScrollableTabRow`, with a `MonarchDurationInput` (with its − and + steppers) per editable
 * row. In the grid each editable cell is a Material 3 outlined field (`BasicTextField` with
 * `OutlinedTextFieldDefaults.DecorationBox`); the text is read when the cell loses focus or on the Next key,
 * which also moves down. Text that cannot be read, a value above [maximumMinutesPerDay] and a failed save keep
 * the text, mark the cell as an error and show the message under the grid in a polite live region.
 *
 * The grid does not scroll vertically: place it in a scrolling screen. Days are day keys: any time on that
 * date in UTC as epoch milliseconds, as in the Material 3 `DatePickerState`, formatted in the configuration's
 * locale.
 *
 * @param days the days in order, usually a week.
 * @param rows the rows with their minutes per day.
 * @param modifier the modifier of the grid.
 * @param summaryRows display-only footer rows with text per day, such as attendance.
 * @param maximumMinutesPerDay the largest value a cell accepts, and the day total above which a day is flagged.
 * @param layout the grid, one day at a time, or whichever fits.
 * @param readOnly makes every cell read-only, for example after submission. Wins over [onCellChange].
 * @param emptyMessage replaces the built-in "No time logged for this period." shown without rows.
 * @param labels replacements for the built-in strings and formats.
 * @param actionsIcon the icon of each row's actions button, for example a three-dot mark. Without it the button
 * shows the text "Actions".
 * @param rowActions the actions of a row, in a menu beside its name, such as clearing the row.
 * @param onDayActivate makes day headings buttons, for example to open a day view. Receives the day key.
 * @param onCellActivate makes read-only cells with time buttons, for example to open the entries behind them.
 * Receives the row id and the day key.
 * @param onCellChange saves a typed value: the row id, the day key and the minutes (0 clears). The cell is
 * disabled and read as "Saving" while it runs; a thrown exception's message is shown on the cell. Without it,
 * every cell is read-only.
 */
@ExperimentalMonarchApi
@Suppress("LongParameterList")
@Composable
public fun MonarchTimesheetGrid(
    days: List<MonarchTimesheetGridDay>,
    rows: List<MonarchTimesheetGridRow>,
    modifier: Modifier = Modifier,
    summaryRows: List<MonarchTimesheetGridSummaryRow> = emptyList(),
    maximumMinutesPerDay: Int = DefaultMaximumMinutesPerDay,
    layout: MonarchTimesheetGridLayout = MonarchTimesheetGridLayout.Automatic,
    readOnly: Boolean = false,
    emptyMessage: String? = null,
    labels: MonarchTimesheetGridLabels = MonarchTimesheetGridLabels(),
    actionsIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.More,
    rowActions: ((MonarchTimesheetGridRow) -> List<MonarchDataTableRowAction>)? = null,
    onDayActivate: ((day: Long) -> Unit)? = null,
    onCellActivate: ((rowId: String, day: Long) -> Unit)? = null,
    onCellChange: (suspend (rowId: String, day: Long, minutes: Int) -> Unit)? = null,
) {
    val text = rememberTimesheetGridText(labels)
    val scope = rememberCoroutineScope()
    val editor = remember(scope) { TimesheetGridEditor(scope) }
    val model = TimesheetGridModel(
        days = days,
        rows = rows,
        summaryRows = summaryRows,
        maximum = maximumMinutesPerDay,
        editable = TimesheetGridRules.editable(readOnly, onCellChange != null),
        emptyMessage = emptyMessage ?: text.empty,
        text = text,
        editor = editor,
        rowActions = rowActions,
        actionsIcon = actionsIcon,
        onDayActivate = onDayActivate,
        onCellActivate = onCellActivate,
        onCellChange = onCellChange,
    )
    Column(modifier = modifier.fillMaxWidth()) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val rules = TimesheetGridLayoutRules
            val fits = rules.fitsGrid(maxWidth, days.size, LocalDensity.current.fontScale)
            when (rules.resolve(layout, fits)) {
                MonarchTimesheetGridLayout.Grid -> TimesheetGridTable(model, rules.rowHeaderWidth(maxWidth, days.size))
                else -> TimesheetGridDayPages(model)
            }
        }
        // Always present, so TalkBack announces each new message politely.
        Text(
            text = editor.message.orEmpty(),
            modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
            style = MonarchTheme.typography.caption,
            color = MonarchTimesheetGridTokens.error(MonarchTheme.colors),
        )
    }
}

/** A calendar day in minutes: the default daily maximum. */
private const val DefaultMaximumMinutesPerDay = 1440
