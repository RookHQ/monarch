package com.rookhq.monarch.components.geo.phonecountryselect

import androidx.compose.runtime.Immutable
import com.rookhq.monarch.components.forms.combobox.MonarchComboboxLabels

/**
 * Replacements for the built-in strings of a `MonarchPhoneCountrySelect`.
 * Every `null` keeps the built-in string `monarch_phonecountryselect_<key>`.
 *
 * @property label the label of the field and the title of the sheet: "Country code".
 * @property suggested the heading over the `preferred` countries: "Suggested".
 * @property all the heading over the other countries while `preferred` is set: "All countries".
 * @property empty shown when no country matches the search: "No matching country".
 * @property option the text of a row from the country's name and its calling code: "Portugal (+351)".
 * @property combobox the strings of the underlying `MonarchCombobox` (search, cancel, clear). Its `empty` is replaced
 * by [empty].
 */
@Immutable
public class MonarchPhoneCountrySelectLabels(
    public val label: String? = null,
    public val suggested: String? = null,
    public val all: String? = null,
    public val empty: String? = null,
    public val option: ((name: String, dialCode: String) -> String)? = null,
    public val combobox: MonarchComboboxLabels = MonarchComboboxLabels(),
)
