package dev.monarchui.components.workflow.timesheetdiff

import androidx.compose.runtime.Immutable

/**
 * An entry present in both versions with different values.
 *
 * @property id the entry's id.
 * @property fields the names of the fields that changed, from [MonarchTimesheetDiffFields] or the product's own.
 * @property before the entry as it was.
 * @property after the entry as it is now.
 */
@Immutable
public class MonarchTimesheetDiffChange(
    public val id: String,
    public val fields: List<String>,
    public val before: MonarchTimesheetDiffEntry,
    public val after: MonarchTimesheetDiffEntry,
)
