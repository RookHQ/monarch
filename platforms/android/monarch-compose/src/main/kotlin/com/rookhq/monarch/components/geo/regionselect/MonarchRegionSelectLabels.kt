package com.rookhq.monarch.components.geo.regionselect

import androidx.compose.runtime.Immutable
import com.rookhq.monarch.components.forms.combobox.MonarchComboboxLabels

/**
 * Replacements for the built-in strings of a `MonarchRegionSelect`. Every
 * `null` keeps the built-in string `monarch_regionselect_<key>`.
 *
 * @property label the label of the field for every country. By default the country's own word: "State" for the
 * United States, "Province or territory" for Canada, "State or territory" for Australia, "Nation" for the United
 * Kingdom, "State or union territory" for India, "Region" otherwise.
 * @property chooseCountryFirst the text under the disabled field while no country is chosen, unless `helperText` is
 * given: "Choose a country first".
 * @property textEntry the placeholder of the text field used when the country has no list: "Type the region".
 * @property empty shown when no region matches the search: "No regions match".
 * @property loadFailed shown when `loadRegions` fails, with a retry button: "Couldn’t load the regions.".
 * @property combobox the strings of the underlying `MonarchCombobox` (search, loading, cancel, clear). Its `empty` is
 * replaced by [empty].
 */
@Immutable
public class MonarchRegionSelectLabels(
    public val label: String? = null,
    public val chooseCountryFirst: String? = null,
    public val textEntry: String? = null,
    public val empty: String? = null,
    public val loadFailed: String? = null,
    public val combobox: MonarchComboboxLabels = MonarchComboboxLabels(),
)
