package com.rookhq.monarch.components.geo.currencyselect

import androidx.compose.runtime.Immutable
import com.rookhq.monarch.components.forms.combobox.MonarchComboboxLabels

/**
 * Replacements for the built-in strings of a `MonarchCurrencySelect`. Every
 * `null` keeps the built-in string `monarch_currencyselect_<key>`.
 *
 * @property label the label of the field and the title of the sheet: "Currency".
 * @property suggested the heading over the `priority` currencies: "Suggested".
 * @property all the heading over the other currencies while `priority` is set: "All currencies".
 * @property empty shown when no currency matches the search: "No currencies match".
 * @property combobox the strings of the underlying `MonarchCombobox` (search, cancel, clear). Its `empty` is replaced
 * by [empty].
 */
@Immutable
public class MonarchCurrencySelectLabels(
    public val label: String? = null,
    public val suggested: String? = null,
    public val all: String? = null,
    public val empty: String? = null,
    public val combobox: MonarchComboboxLabels = MonarchComboboxLabels(),
)
