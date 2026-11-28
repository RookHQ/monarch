package com.rookhq.monarch.components.workflow.timesheetgrid

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.datagrid.datatable.MonarchDataTableRowAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/** A cell: a row and a day key. */
internal data class TimesheetGridCellKey(val rowId: String, val day: Long)

/**
 * The edit of a cell that is not yet the row's value.
 *
 * @property draft the text kept on screen (a rejected or just-saved text), or `null` for the row's value.
 * @property base the row's value the draft was made against. When the row's value changes, the draft is dropped.
 * @property busy the value is being saved.
 * @property error the message of a rejected text or a failed save.
 */
@Immutable
internal data class TimesheetGridCellEdit(
    val draft: String? = null,
    val base: Int = 0,
    val busy: Boolean = false,
    val error: String? = null,
)

/** Holds the drafts, saves in progress and errors of the cells, and runs the app's save. */
@Stable
internal class TimesheetGridEditor(private val scope: CoroutineScope) {
    private val edits = mutableStateMapOf<TimesheetGridCellKey, TimesheetGridCellEdit>()

    /** The latest error, shown under the grid and announced. */
    var message: String? by mutableStateOf(null)
        private set

    /** The edit of [key] while the row's value is [saved]; a draft made against another value is dropped. */
    fun state(key: TimesheetGridCellKey, saved: Int): TimesheetGridCellEdit {
        val edit = edits[key] ?: return TimesheetGridCellEdit(base = saved)
        return if (edit.base == saved || edit.busy) edit else TimesheetGridCellEdit(base = saved)
    }

    /** Drops the draft and error of [key]. */
    fun revert(key: TimesheetGridCellKey) {
        if (edits.remove(key)?.error != null) message = null
    }

    /** Reads typed [text] and saves it, or keeps it with a message. */
    @Suppress("LongParameterList")
    fun commit(
        key: TimesheetGridCellKey,
        saved: Int,
        text: String,
        maximum: Int,
        words: TimesheetGridText,
        onSave: suspend (String, Long, Int) -> Unit,
    ) {
        if (state(key, saved).busy) return
        when (val result = TimesheetGridRules.commit(text, saved, maximum)) {
            TimesheetGridCommit.Unchanged -> revert(key)
            TimesheetGridCommit.Invalid -> fail(key, saved, text, words.invalid)
            TimesheetGridCommit.TooLong -> fail(key, saved, text, words.tooLong(words.duration(maximum)))
            is TimesheetGridCommit.Save -> save(key, saved, result.minutes, text, words.saveFailed, onSave)
        }
    }

    /** Saves [minutes] through [onSave]; the cell is busy until it returns, and shows its failure. */
    @Suppress("LongParameterList")
    fun save(
        key: TimesheetGridCellKey,
        saved: Int,
        minutes: Int,
        text: String?,
        saveFailed: String,
        onSave: suspend (String, Long, Int) -> Unit,
    ) {
        if (state(key, saved).busy) return
        if (minutes == saved) {
            revert(key)
            return
        }
        edits[key] = TimesheetGridCellEdit(draft = text, base = saved, busy = true)
        scope.launch {
            val failure = attempt(saveFailed) { onSave(key.rowId, key.day, minutes) }
            // On success the typed text stays until the app passes the new value in the rows.
            edits[key] = TimesheetGridCellEdit(draft = text, base = saved, error = failure)
            message = failure
        }
    }

    private fun fail(key: TimesheetGridCellKey, saved: Int, text: String, error: String) {
        edits[key] = TimesheetGridCellEdit(draft = text, base = saved, error = error)
        message = error
    }

    /** Runs the app's save; any failure becomes its message, or [fallback] without one. Cancellation is rethrown. */
    @Suppress("TooGenericExceptionCaught")
    private suspend fun attempt(fallback: String, block: suspend () -> Unit): String? = try {
        block()
        null
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        failure.message?.takeIf { it.isNotBlank() } ?: fallback
    }
}

/** Everything the layouts of a timesheet grid need. */
@Suppress("LongParameterList")
internal class TimesheetGridModel(
    val days: List<MonarchTimesheetGridDay>,
    val rows: List<MonarchTimesheetGridRow>,
    val summaryRows: List<MonarchTimesheetGridSummaryRow>,
    val maximum: Int,
    val editable: Boolean,
    val emptyMessage: String,
    val text: TimesheetGridText,
    val editor: TimesheetGridEditor,
    val rowActions: ((MonarchTimesheetGridRow) -> List<MonarchDataTableRowAction>)?,
    val actionsIcon: (@Composable () -> Unit)?,
    val onDayActivate: ((Long) -> Unit)?,
    val onCellActivate: ((String, Long) -> Unit)?,
    val onCellChange: (suspend (String, Long, Int) -> Unit)?,
) {
    /** The key of a cell. */
    fun key(row: MonarchTimesheetGridRow, day: MonarchTimesheetGridDay): TimesheetGridCellKey =
        TimesheetGridCellKey(row.id, TimesheetGridRules.dayKey(day.date))

    /** The lock reason of a locked day, "Locked" without one, or `null` when the day is open. */
    fun lockText(day: MonarchTimesheetGridDay): String? =
        if (TimesheetGridRules.locked(day)) day.lockReason ?: text.locked else null

    /** A day read in full: "Mon, Jan 12, Today, Approved, Holiday". */
    fun spokenDay(day: MonarchTimesheetGridDay): String = listOfNotNull(
        text.longDay(day.date),
        text.today.takeIf { day.today },
        lockText(day),
        day.note,
    ).joinToString(", ")

    /** The name of a cell: "Atlas, Mon, Jan 12". */
    fun cellLabel(row: MonarchTimesheetGridRow, day: MonarchTimesheetGridDay): String =
        text.cellLabel(row.label, text.longDay(day.date))

    /** Minutes for the eye, a dash when there are none. */
    fun shown(minutes: Int): String = if (minutes > 0) text.duration(minutes) else MonarchTimesheetGridTokens.EMPTY_MARK

    /** Minutes for TalkBack, "No time" when there are none. */
    fun spoken(minutes: Int): String = if (minutes > 0) text.spokenDuration(minutes) else text.noTime

    /** The value of a read-only cell for TalkBack, with the day's lock reason. */
    fun spokenCell(minutes: Int, day: MonarchTimesheetGridDay): String =
        listOfNotNull(spoken(minutes), lockText(day)).joinToString(", ")

    /** A day total for TalkBack: the time, the expected time and whether it is over the limit. */
    fun spokenDayTotal(total: Int, day: MonarchTimesheetGridDay): String = listOfNotNull(
        spoken(total),
        day.expectedMinutes?.let { text.expected(text.spokenDuration(it)) },
        text.overLimit.takeIf { TimesheetGridRules.over(total, maximum) },
    ).joinToString(", ")

    /** Commits typed [value] of a grid cell. */
    fun commit(row: MonarchTimesheetGridRow, day: MonarchTimesheetGridDay, value: String) {
        val save = onCellChange ?: return
        editor.commit(key(row, day), row.minutes(day.date), value, maximum, text, save)
    }

    /** Saves [minutes] of a day-layout field, already read and checked by the field. */
    fun save(row: MonarchTimesheetGridRow, day: MonarchTimesheetGridDay, minutes: Int) {
        val save = onCellChange ?: return
        editor.save(key(row, day), row.minutes(day.date), minutes, null, text.saveFailed, save)
    }
}
