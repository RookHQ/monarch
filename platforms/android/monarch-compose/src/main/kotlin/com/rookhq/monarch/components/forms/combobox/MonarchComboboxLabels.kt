package com.rookhq.monarch.components.forms.combobox

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchCombobox`. Every `null`
 * keeps the built-in string `monarch_combobox_<key>`.
 *
 * @property search the placeholder of the search field: "Search".
 * @property loading shown while options load and none match yet: "Loading…".
 * @property empty shown when no option matches: "No matches".
 * @property create the option that creates a value from the typed text: "Create “<text>”".
 * @property more shown under a list cut short by `limit`, from the number shown and the number of matches:
 * "Showing 50 of 120. Type to narrow the list.".
 * @property cancel the button that closes the sheet without choosing: "Cancel".
 * @property clear the spoken name of the clear button: "Clear".
 * @property failed shown when the options could not be loaded and the app gave no message: "The options could not
 * be loaded.".
 * @property retry the button after the load error: "Try again".
 */
@Immutable
public class MonarchComboboxLabels(
    public val search: String? = null,
    public val loading: String? = null,
    public val empty: String? = null,
    public val create: ((text: String) -> String)? = null,
    public val more: ((shown: Int, total: Int) -> String)? = null,
    public val cancel: String? = null,
    public val clear: String? = null,
    public val failed: String? = null,
    public val retry: String? = null,
)
