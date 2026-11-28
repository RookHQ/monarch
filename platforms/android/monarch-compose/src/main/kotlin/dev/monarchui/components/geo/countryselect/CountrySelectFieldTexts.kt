package dev.monarchui.components.geo.countryselect

import dev.monarchui.components.forms.combobox.MonarchComboboxLabels

/** The texts and options of a geo picker's field, besides its list. */
internal class CountrySelectFieldTexts(
    val label: String,
    val suggested: String,
    val all: String,
    val placeholder: String?,
    val helperText: String?,
    val errorText: String?,
    val clearOptionLabel: String?,
    val combobox: MonarchComboboxLabels,
    val labelVisible: Boolean = true,
)
