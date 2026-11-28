package com.rookhq.monarch.components.forms.numberinput

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchNumberInput`. Every
 * `null` keeps the built-in string `monarch_numberinput_<key>`.
 *
 * @property increment the spoken name of the + button and the field's increase action ("Increase").
 * @property decrement the spoken name of the − button and the field's decrease action ("Decrease").
 * @property invalid the message for text that is not a number ("Enter a number.").
 * @property range the message for a number outside the bounds, given the bounds already formatted, `null` for a
 * missing one. By default "Enter a number from 0 to 99.", "Enter 0 or more." or "Enter 99 or less."
 */
@Immutable
public class MonarchNumberInputLabels(
    public val increment: String? = null,
    public val decrement: String? = null,
    public val invalid: String? = null,
    public val range: ((minimum: String?, maximum: String?) -> String)? = null,
)
