package dev.monarchui.components.workflow.timesheetdiff

/**
 * The names of the fields a `MonarchTimesheetDiffChange` lists. The first
 * seven are compared by `MonarchTimesheetDiffResult.between`; [START] and
 * [END] have built-in names for products that store clock times. A product
 * may use its own names; they are shown as given unless
 * `MonarchTimesheetDiffLabels.fields` names them.
 */
public object MonarchTimesheetDiffFields {
    /** The day worked. Shown as "date". */
    public const val DATE: String = "date"

    /** The time worked. Shown as "time". */
    public const val MINUTES: String = "minutes"

    /** The project. Shown as "project". */
    public const val PROJECT: String = "project"

    /** The activity. Shown as "activity". */
    public const val ACTIVITY: String = "activity"

    /** The task. Shown as "task". */
    public const val TASK: String = "task"

    /** Whether the time is billable. Shown as "billable". */
    public const val BILLABLE: String = "billable"

    /** The note. Shown as "note". */
    public const val NOTE: String = "note"

    /** A start time. Shown as "start". */
    public const val START: String = "start"

    /** An end time. Shown as "end". */
    public const val END: String = "end"
}
