package dev.monarchui.components.geo.addressautocomplete

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.monarchui.ExperimentalMonarchApi
import java.util.Locale

/**
 * The same address field as [MonarchAddressAutocomplete], laid out as the web does: an editable search field with
 * the typed text and the search icon, the suggestions in a card under it (and not in a bottom sheet), and "Enter
 * the address manually" below. Typing or focusing the field opens the card; a pick, the manual row or the keyboard's
 * Done closes it. Everything else, including [search], [resolve] and the form after a pick, works as in
 * [MonarchAddressAutocomplete].
 *
 * @param address the address. A pick replaces it; the form edits its parts.
 * @param onAddressChange called with the new address after a pick or an edit of the form.
 * @param search returns the suggestions for the trimmed text. Throw to show "Couldn’t search addresses." with Try
 * again.
 * @param modifier the modifier of the component.
 * @param mode the search field only, or with the form, when the app owns it. `null` keeps it inside.
 * @param onModeChange called when the mode changes.
 * @param enabled whether the field, the form and the buttons can be used.
 * @param allowManualEntry offers "Enter the address manually" in the card and under the field.
 * @param minQueryLength the characters needed before [search] runs.
 * @param debounceMillis how long to wait after typing before [search] runs.
 * @param locale the language of the country names. `null` uses the first locale of the configuration.
 * @param helperText guidance under the search field.
 * @param errorText an error under the search field; the field is in the error state.
 * @param fieldErrors errors of the form's fields, shown under each field.
 * @param resolve turns a picked suggestion without an address into one. Throw to show the resolve error.
 * @param onSuggestionSelect receives the picked suggestion and the address it gave.
 * @param searchIcon an icon at the start of the search field. Monarch bundles no icons.
 * @param suggestionIcon a decorative icon at the start of each suggestion, such as a pin.
 * @param labels replacements for the built-in strings.
 * @param required marks the field as required: an asterisk after the label.
 * @param initialQuery the text in the search field when it first appears.
 * @param initiallyOpen opens the suggestions when the field first appears, as the web example does.
 */
@ExperimentalMonarchApi
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongParameterList", "LongMethod")
public fun MonarchAddressAutocompleteInline(
    address: MonarchAddressAutocompleteAddress,
    onAddressChange: (MonarchAddressAutocompleteAddress) -> Unit,
    search: suspend (query: String) -> List<MonarchAddressAutocompleteSuggestion>,
    modifier: Modifier = Modifier,
    mode: MonarchAddressAutocompleteMode? = null,
    onModeChange: ((MonarchAddressAutocompleteMode) -> Unit)? = null,
    enabled: Boolean = true,
    allowManualEntry: Boolean = true,
    minQueryLength: Int = 3,
    debounceMillis: Long = 300L,
    locale: Locale? = null,
    helperText: String? = null,
    errorText: String? = null,
    fieldErrors: Map<MonarchAddressAutocompleteField, String> = emptyMap(),
    resolve: (suspend (MonarchAddressAutocompleteSuggestion) -> MonarchAddressAutocompleteAddress)? = null,
    onSuggestionSelect: ((MonarchAddressAutocompleteSuggestion, MonarchAddressAutocompleteAddress) -> Unit)? = null,
    searchIcon: (@Composable () -> Unit)? = null,
    suggestionIcon: (@Composable () -> Unit)? = null,
    labels: MonarchAddressAutocompleteLabels = MonarchAddressAutocompleteLabels(),
    required: Boolean = false,
    initialQuery: String = "",
    initiallyOpen: Boolean = false,
) {
    AddressAutocompleteBody(
        address = address,
        onAddressChange = onAddressChange,
        search = search,
        modifier = modifier,
        mode = mode,
        onModeChange = onModeChange,
        enabled = enabled,
        allowManualEntry = allowManualEntry,
        minQueryLength = minQueryLength,
        debounceMillis = debounceMillis,
        locale = locale,
        helperText = helperText,
        errorText = errorText,
        fieldErrors = fieldErrors,
        resolve = resolve,
        onSuggestionSelect = onSuggestionSelect,
        searchIcon = searchIcon,
        suggestionIcon = suggestionIcon,
        labels = labels,
        required = required,
        initialQuery = initialQuery,
        initiallyOpen = initiallyOpen,
        inline = true,
    )
}
