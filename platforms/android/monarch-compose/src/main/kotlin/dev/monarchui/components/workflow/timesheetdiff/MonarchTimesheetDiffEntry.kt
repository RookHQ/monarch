package dev.monarchui.components.workflow.timesheetdiff

import androidx.compose.runtime.Immutable

/**
 * One time entry as it was in one version of a timesheet.
 *
 * @property id a stable identifier, the same in both versions for an entry that changed.
 * @property date the day worked: the epoch millisecond of its midnight in UTC, as in the Material 3
 * `DatePickerState`. Any time on that UTC day is the same day.
 * @property minutes the time worked, in minutes.
 * @property project the project, client or cost centre. `null` or empty reads "No project".
 * @property activity the activity or category.
 * @property task the task, issue key or short description.
 * @property billable whether the time is billable; `null` when billing does not apply, which counts as billable
 * in the totals, as on the web.
 * @property note a note in the person's own words.
 */
@Immutable
public class MonarchTimesheetDiffEntry(
    public val id: String,
    public val date: Long,
    public val minutes: Int,
    public val project: String? = null,
    public val activity: String? = null,
    public val task: String? = null,
    public val billable: Boolean? = null,
    public val note: String? = null,
)
