package dev.monarchui.components.forms.multiselect

import androidx.compose.runtime.Immutable
import dev.monarchui.components.forms.combobox.MonarchComboboxLabels

/**
 * Replacements for the built-in strings of a `MonarchMultiSelect`. Every
 * `null` uses the built-in string named after it in `strings_multiselect.xml`.
 *
 * @property select the button's text while nothing is chosen: "Select…". The `placeholder` parameter wins.
 * @property change the button's text once something is chosen: "Edit selection".
 * @property done the button that closes the sheet: "Done".
 * @property none the button's spoken value while nothing is chosen: "None".
 * @property count writes the count when there is a limit, from the number chosen and the limit: "2 of 3 selected".
 * @property remove writes the spoken name of a chosen value's remove button, from its label: "Remove Design".
 * @property list the strings of the searchable list, shared with `MonarchCombobox`: search, loading, empty, create
 * and more.
 */
@Immutable
public class MonarchMultiSelectLabels(
    public val select: String? = null,
    public val change: String? = null,
    public val done: String? = null,
    public val none: String? = null,
    public val count: ((count: Int, max: Int) -> String)? = null,
    public val remove: ((label: String) -> String)? = null,
    public val list: MonarchComboboxLabels = MonarchComboboxLabels(),
)
