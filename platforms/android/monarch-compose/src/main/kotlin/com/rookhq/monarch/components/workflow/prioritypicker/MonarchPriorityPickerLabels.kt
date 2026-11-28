package com.rookhq.monarch.components.workflow.prioritypicker

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchPriorityPicker`. Every
 * `null` keeps the built-in string `monarch_prioritypicker_<key>`.
 *
 * @property trigger what TalkBack reads for the trigger, given the current level's name: "Priority: High".
 * @property urgent the name of [MonarchPriorityLevel.Urgent], "Urgent".
 * @property high the name of [MonarchPriorityLevel.High], "High".
 * @property medium the name of [MonarchPriorityLevel.Medium], "Medium".
 * @property low the name of [MonarchPriorityLevel.Low], "Low".
 * @property none the name of [MonarchPriorityLevel.None], "No priority".
 */
@Immutable
public class MonarchPriorityPickerLabels(
    public val trigger: ((String) -> String)? = null,
    public val urgent: String? = null,
    public val high: String? = null,
    public val medium: String? = null,
    public val low: String? = null,
    public val none: String? = null,
)
