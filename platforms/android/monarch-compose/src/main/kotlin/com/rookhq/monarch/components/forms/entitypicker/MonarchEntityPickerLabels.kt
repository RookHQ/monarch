package com.rookhq.monarch.components.forms.entitypicker

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchEntityPicker`. Every
 * `null` uses the built-in string named after it in `strings_entitypicker.xml`.
 *
 * @property select the field's text while nothing is chosen: "Select…". The `placeholder` parameter wins.
 * @property search the placeholder of the sheet's search field: "Search".
 * @property cancel the button that closes a single-choice sheet: "Cancel".
 * @property done the button that closes a multiple-choice sheet: "Done".
 * @property loading the list's text while entities load or a search runs: "Loading…".
 * @property empty the list's text when nothing matches: "No matches".
 * @property tooShort writes the list's text while the search is too short, from the minimum length.
 * @property failed the list's text when a search failed: "The results could not be loaded.".
 * @property retry the button that runs a failed search again. `null` uses `MonarchInlineErrorRetry`'s "Try again".
 * @property create writes the row that creates an entity, from the typed text: "Create “Acme”".
 * @property summary writes the field with several chosen, from the first name and the number of others.
 * @property clear the spoken name of the clear button: "Clear".
 * @property none the field's spoken value while nothing is chosen: "None".
 */
@Immutable
public class MonarchEntityPickerLabels(
    public val select: String? = null,
    public val search: String? = null,
    public val cancel: String? = null,
    public val done: String? = null,
    public val loading: String? = null,
    public val empty: String? = null,
    public val tooShort: ((minLength: Int) -> String)? = null,
    public val failed: String? = null,
    public val retry: String? = null,
    public val create: ((text: String) -> String)? = null,
    public val summary: ((first: String, others: Int) -> String)? = null,
    public val clear: String? = null,
    public val none: String? = null,
)
