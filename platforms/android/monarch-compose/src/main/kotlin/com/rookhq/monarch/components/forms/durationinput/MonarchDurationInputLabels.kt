package com.rookhq.monarch.components.forms.durationinput

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchDurationInput`. Every
 * `null` keeps the built-in string `monarch_durationinput_<key>`.
 *
 * @property invalid the message for text that is not a duration: "Use a duration like 1h 30m, 90m or 1.5h.".
 * @property range the message for a duration outside the bounds, given the minimum and the maximum in minutes
 * (`null` without a maximum). By default "Enter between 15m and 2h.", "Enter at least 15m." or
 * "Enter at most 2h.", with the bounds written by the input's format.
 * @property valueText the spoken duration, from the minutes. By default the locale's words, such as
 * "1 hour, 30 minutes".
 * @property increment the spoken name of the + button and the field's increase action: "Increase".
 * @property decrement the spoken name of the − button and the field's decrease action: "Decrease".
 */
@Immutable
public class MonarchDurationInputLabels(
    public val invalid: String? = null,
    public val range: ((minimum: Int, maximum: Int?) -> String)? = null,
    public val valueText: ((minutes: Int) -> String)? = null,
    public val increment: String? = null,
    public val decrement: String? = null,
)
