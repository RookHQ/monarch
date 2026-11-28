package dev.monarchui.components.forms.ratingpicker

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchRatingPicker`. Every
 * `null` keeps the built-in string `monarch_ratingpicker_<key>`.
 *
 * @property valueText the spoken value of a star, or of a read-only picker, given the value (`null` for no rating),
 * the last value of the scale and the value's label. By default "3 of 5", "3 of 5, Good" or "No rating".
 * @property clear the name of the action that clears a clearable rating ("Clear rating").
 */
@Immutable
public class MonarchRatingPickerLabels(
    public val valueText: ((value: Int?, max: Int, label: String?) -> String)? = null,
    public val clear: String? = null,
)
