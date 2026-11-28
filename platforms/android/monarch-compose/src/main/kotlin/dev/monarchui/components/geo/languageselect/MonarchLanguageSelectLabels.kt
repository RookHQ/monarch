package dev.monarchui.components.geo.languageselect

import androidx.compose.runtime.Immutable
import dev.monarchui.components.forms.combobox.MonarchComboboxLabels

/**
 * Replacements for the built-in strings of a `MonarchLanguageSelect`. Every
 * `null` keeps the built-in string `monarch_languageselect_<key>`.
 *
 * @property label the label of the field and the title of the sheet: "Language".
 * @property suggested the heading over the `priority` languages: "Suggested".
 * @property all the heading over the other languages while `priority` is set: "All languages".
 * @property empty shown when no language matches the search: "No languages match".
 * @property combobox the strings of the underlying `MonarchCombobox` (search, cancel, clear). Its `empty` is replaced
 * by [empty].
 */
@Immutable
public class MonarchLanguageSelectLabels(
    public val label: String? = null,
    public val suggested: String? = null,
    public val all: String? = null,
    public val empty: String? = null,
    public val combobox: MonarchComboboxLabels = MonarchComboboxLabels(),
)
