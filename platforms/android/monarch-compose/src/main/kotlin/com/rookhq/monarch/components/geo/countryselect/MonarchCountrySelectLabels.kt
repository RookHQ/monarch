package com.rookhq.monarch.components.geo.countryselect

import androidx.compose.runtime.Immutable
import com.rookhq.monarch.components.forms.combobox.MonarchComboboxLabels

/**
 * Replacements for the built-in strings of a `MonarchCountrySelect`. Every
 * `null` keeps the built-in string `monarch_countryselect_<key>`.
 *
 * @property label the label of the field and the title of the sheet: "Country".
 * @property suggested the heading over the `priority` countries: "Suggested".
 * @property all the heading over the other countries while `priority` is set: "All countries".
 * @property empty shown when no country matches the search: "No countries match".
 * @property combobox the strings of the underlying `MonarchCombobox` (search, cancel, clear). Its `empty` is replaced
 * by [empty].
 */
@Immutable
public class MonarchCountrySelectLabels(
    public val label: String? = null,
    public val suggested: String? = null,
    public val all: String? = null,
    public val empty: String? = null,
    public val combobox: MonarchComboboxLabels = MonarchComboboxLabels(),
)
