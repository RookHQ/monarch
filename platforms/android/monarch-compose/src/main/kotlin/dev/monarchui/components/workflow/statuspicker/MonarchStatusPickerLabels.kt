package dev.monarchui.components.workflow.statuspicker

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchStatusPicker`. Every
 * `null` keeps the built-in string `monarch_statuspicker_<key>`.
 *
 * @property trigger what TalkBack reads for the trigger, given the current status's name: "Status: In progress".
 * @property none the name shown when no status is selected, "No status".
 * @property backlog the menu heading of [MonarchStatusCategory.Backlog], "Backlog".
 * @property unstarted the menu heading of [MonarchStatusCategory.Unstarted], "Not started".
 * @property started the menu heading of [MonarchStatusCategory.Started], "In progress".
 * @property completed the menu heading of [MonarchStatusCategory.Completed], "Completed".
 * @property cancelled the menu heading of [MonarchStatusCategory.Cancelled], "Cancelled".
 */
@Immutable
public class MonarchStatusPickerLabels(
    public val trigger: ((String) -> String)? = null,
    public val none: String? = null,
    public val backlog: String? = null,
    public val unstarted: String? = null,
    public val started: String? = null,
    public val completed: String? = null,
    public val cancelled: String? = null,
)
