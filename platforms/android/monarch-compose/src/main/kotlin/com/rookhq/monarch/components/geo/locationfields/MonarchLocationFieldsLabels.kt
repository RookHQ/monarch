package com.rookhq.monarch.components.geo.locationfields

import androidx.compose.runtime.Immutable
import com.rookhq.monarch.components.forms.combobox.MonarchComboboxLabels

/**
 * Replacements for the built-in strings of `MonarchLocationFields`. Every
 * `null` keeps the built-in text.
 *
 * @property country the label of the country field. Default "Country".
 * @property region the label of the region field. Default: the country's own word, such as "State".
 * @property city the label of the city field. Default "City".
 * @property postalCode the label of the postal code field. Default "Postal code".
 * @property chooseCountryFirst the help under the city search while no country is chosen.
 * @property typeMore shown in the city list until enough letters are typed; receives the minimum.
 * @property noMatch shown in the city list when no city matches.
 * @property citiesLoadFailed shown in the city list when the search failed.
 * @property useTyped the row that keeps the typed city as it is; receives the typed text. Default "Use “text”".
 * @property combobox replacements for the strings of the city combobox.
 */
@Immutable
public class MonarchLocationFieldsLabels(
    public val country: String? = null,
    public val region: String? = null,
    public val city: String? = null,
    public val postalCode: String? = null,
    public val chooseCountryFirst: String? = null,
    public val typeMore: ((minLength: Int) -> String)? = null,
    public val noMatch: String? = null,
    public val citiesLoadFailed: String? = null,
    public val useTyped: ((text: String) -> String)? = null,
    public val combobox: MonarchComboboxLabels = MonarchComboboxLabels(),
)
