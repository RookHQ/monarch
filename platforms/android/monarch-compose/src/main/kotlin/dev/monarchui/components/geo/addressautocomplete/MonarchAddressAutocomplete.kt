package dev.monarchui.components.geo.addressautocomplete

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.datadisplay.progress.MonarchProgress
import dev.monarchui.components.forms.formfield.MonarchFieldFrame
import dev.monarchui.components.forms.formfield.monarchFieldSemantics
import dev.monarchui.components.forms.textfield.MonarchFieldTrigger
import dev.monarchui.internal.MonarchLiveStatus
import dev.monarchui.internal.monarchString
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * An address field with suggestions from the app's own provider: the person
 * searches, picks a suggestion, and the street, city, region, postal code and
 * country fill in for review. Manual entry is always one step away.
 *
 * Monarch makes no requests and ships no map or places SDK: [search] and
 * [resolve] are the app's suspending functions (its own endpoint, the Places
 * SDK, or `android.location.Geocoder` behind them). The field is a read-only
 * Monarch field box with the drop-down role that opens a `MonarchBottomSheet`
 * with a `MonarchSearchBar`. The text is searched
 * after [debounceMillis] once it has [minQueryLength] characters; a newer
 * text cancels the older search, and a failed search shows Try again. After a
 * pick the form shows the address for review: `MonarchTextField`s with the
 * autofill content types of an address and a `MonarchCountrySelect`. "Enter
 * the address manually" opens the form with the typed text as the street
 * line; "Search for an address" closes it.
 *
 * A product composite: experimental until a Rook app adopts it.
 *
 * @param address the address. A pick replaces it; the form edits its parts.
 * @param onAddressChange called with the new address after a pick or an edit of the form.
 * @param search returns the suggestions for the trimmed text. Throw to show "Couldn’t search addresses." with Try
 * again. It is cancelled when the text changes or the sheet closes.
 * @param modifier the modifier of the component.
 * @param mode the search field only, or with the form, when the app owns it. `null` keeps it inside, starting on the
 * form when the address has a street line.
 * @param onModeChange called when the mode changes.
 * @param enabled whether the field, the form and the buttons can be used.
 * @param allowManualEntry offers "Enter the address manually" under the field and at the end of the list.
 * @param minQueryLength the characters needed before [search] runs.
 * @param debounceMillis how long to wait after typing before [search] runs.
 * @param locale the language of the country names. `null` uses the first locale of the configuration.
 * @param helperText guidance under the search field.
 * @param errorText an error under the search field; the field is in the error state.
 * @param fieldErrors errors of the form's fields, shown under each field.
 * @param resolve turns a picked suggestion without an address into one. Throw to show the resolve error. Without it
 * such a suggestion fills only the street line.
 * @param onSuggestionSelect receives the picked suggestion and the address it gave.
 * @param searchIcon an icon at the start of the search field. Monarch bundles no icons.
 * @param suggestionIcon a decorative icon at the start of each suggestion, such as a pin.
 * @param labels replacements for the built-in strings.
 * @param required marks the field as required: an asterisk after the label.
 * @param initialQuery the text in the search box when the search first opens.
 * @param initiallyOpen opens the search when the field first appears, as the web example does.
 */
@ExperimentalMonarchApi
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongParameterList", "LongMethod")
public fun MonarchAddressAutocomplete(
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
        inline = false,
    )
}

/** The shared body of the sheet and the inline layouts. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongParameterList", "LongMethod")
internal fun AddressAutocompleteBody(
    address: MonarchAddressAutocompleteAddress,
    onAddressChange: (MonarchAddressAutocompleteAddress) -> Unit,
    search: suspend (query: String) -> List<MonarchAddressAutocompleteSuggestion>,
    modifier: Modifier,
    mode: MonarchAddressAutocompleteMode? = null,
    onModeChange: ((MonarchAddressAutocompleteMode) -> Unit)?,
    enabled: Boolean = true,
    allowManualEntry: Boolean,
    minQueryLength: Int = 3,
    debounceMillis: Long,
    locale: Locale? = null,
    helperText: String?,
    errorText: String? = null,
    fieldErrors: Map<MonarchAddressAutocompleteField, String>,
    resolve: (suspend (MonarchAddressAutocompleteSuggestion) -> MonarchAddressAutocompleteAddress)? = null,
    onSuggestionSelect: ((MonarchAddressAutocompleteSuggestion, MonarchAddressAutocompleteAddress) -> Unit)?,
    searchIcon: (@Composable () -> Unit)? = null,
    suggestionIcon: (@Composable () -> Unit)?,
    labels: MonarchAddressAutocompleteLabels = MonarchAddressAutocompleteLabels(),
    required: Boolean,
    initialQuery: String = "",
    initiallyOpen: Boolean,
    inline: Boolean,
) {
    val shownLocale = locale ?: LocalConfiguration.current.locales[0]
    var ownMode by rememberSaveable { mutableStateOf(MonarchAddressAutocompleteTokens.initialMode(address)) }
    var open by rememberSaveable { mutableStateOf(initiallyOpen) }
    var query by rememberSaveable { mutableStateOf(initialQuery) }
    var resolving by rememberSaveable { mutableStateOf(false) }
    var resolveError by rememberSaveable { mutableStateOf<String?>(null) }
    var announcement by rememberSaveable { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val shownMode = mode ?: ownMode
    val setMode: (MonarchAddressAutocompleteMode) -> Unit = {
        ownMode = it
        onModeChange?.invoke(it)
    }
    val title = monarchString(labels.label, R.string.monarch_addressautocomplete_label)
    val filled = monarchString(labels.filled, R.string.monarch_addressautocomplete_filled)
    val resolveFailed = monarchString(labels.resolveFailed, R.string.monarch_addressautocomplete_resolve_failed)
    val close = {
        open = false
        query = ""
    }
    val fill: (MonarchAddressAutocompleteSuggestion, MonarchAddressAutocompleteAddress) -> Unit = { suggestion, next ->
        onAddressChange(next)
        setMode(MonarchAddressAutocompleteMode.Manual)
        onSuggestionSelect?.invoke(suggestion, next)
        announcement = filled
    }
    val pick: (MonarchAddressAutocompleteSuggestion) -> Unit = { suggestion ->
        close()
        resolveError = null
        addressAutocompletePick(suggestion, resolve, fill) { work ->
            resolving = true
            scope.launch {
                val failed = work()
                resolving = false
                // The field's error line announces the failure itself, politely, so it is not said twice.
                if (failed) resolveError = resolveFailed
            }
        }
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(MonarchAddressAutocompleteTokens.Gap)) {
        if (inline) {
            AddressAutocompleteInlineSearch(
                frame = AddressAutocompleteInlineFrame(
                    label = title,
                    helperText = helperText,
                    errorText = resolveError ?: errorText,
                    enabled = enabled && !resolving,
                    required = required,
                ),
                state = AddressAutocompleteInlineState(
                    query = query,
                    onQueryChange = {
                        query = it
                        open = true
                    },
                    summary = MonarchAddressAutocompleteTokens.summary(address, shownLocale),
                    placeholder = monarchString(labels.placeholder, R.string.monarch_addressautocomplete_placeholder),
                    open = open && enabled,
                    onOpen = {
                        resolveError = null
                        open = true
                    },
                    onDone = { open = false },
                    searchIcon = searchIcon,
                ),
                searcher = AddressAutocompleteSearcher(search, minQueryLength, debounceMillis),
                texts = addressAutocompleteSheetTexts(labels, title, minQueryLength),
                allowManualEntry = allowManualEntry,
                onPick = pick,
                onManual = {
                    val line1 = MonarchAddressAutocompleteTokens.manualLine1(address.line1, query)
                    onAddressChange(address.copy(line1 = line1))
                    close()
                    setMode(MonarchAddressAutocompleteMode.Manual)
                },
                suggestionIcon = suggestionIcon,
            )
        } else {
            AddressAutocompleteSearchField(
                summary = MonarchAddressAutocompleteTokens.summary(address, shownLocale),
                label = title,
                placeholder = monarchString(labels.placeholder, R.string.monarch_addressautocomplete_placeholder),
                helperText = helperText,
                errorText = resolveError ?: errorText,
                enabled = enabled && !resolving,
                required = required,
                open = open,
                onOpen = {
                    resolveError = null
                    open = true
                },
                searchIcon = searchIcon,
            )
        }
        if (resolving) {
            MonarchProgress(
                modifier = Modifier.fillMaxWidth(),
                label = monarchString(labels.resolving, R.string.monarch_addressautocomplete_resolving),
            )
        }
        AddressAutocompleteBelowField(
            address = address,
            onAddressChange = onAddressChange,
            labels = labels,
            fieldErrors = fieldErrors,
            enabled = enabled,
            locale = locale,
            manual = shownMode == MonarchAddressAutocompleteMode.Manual,
            allowManualEntry = allowManualEntry,
            setMode = setMode,
        )
        // Always present, so TalkBack reads "Address filled in" politely when it changes.
        MonarchLiveStatus(text = announcement.takeIf { it.isNotEmpty() }, visible = false)
    }
    if (open && enabled && !inline) {
        AddressAutocompleteSheet(
            query = query,
            onQueryChange = { query = it },
            searcher = AddressAutocompleteSearcher(search, minQueryLength, debounceMillis),
            texts = addressAutocompleteSheetTexts(labels, title, minQueryLength),
            allowManualEntry = allowManualEntry,
            onPick = pick,
            onManual = {
                val line1 = MonarchAddressAutocompleteTokens.manualLine1(address.line1, query)
                onAddressChange(address.copy(line1 = line1))
                close()
                setMode(MonarchAddressAutocompleteMode.Manual)
            },
            onCancel = close,
            suggestionIcon = suggestionIcon,
        )
    }
}

/** Under the search field: the form and the way back in the manual mode, else "Enter the address manually". */
@Composable
private fun AddressAutocompleteBelowField(
    address: MonarchAddressAutocompleteAddress,
    onAddressChange: (MonarchAddressAutocompleteAddress) -> Unit,
    labels: MonarchAddressAutocompleteLabels,
    fieldErrors: Map<MonarchAddressAutocompleteField, String>,
    enabled: Boolean,
    locale: Locale?,
    manual: Boolean,
    allowManualEntry: Boolean,
    setMode: (MonarchAddressAutocompleteMode) -> Unit,
) {
    if (manual) {
        AddressAutocompleteForm(
            address = address,
            onAddressChange = onAddressChange,
            texts = addressAutocompleteFormTexts(labels),
            fieldErrors = fieldErrors,
            enabled = enabled,
            locale = locale,
        )
        MonarchButton(
            text = monarchString(labels.backToSearch, R.string.monarch_addressautocomplete_back_to_search),
            onClick = { setMode(MonarchAddressAutocompleteMode.Search) },
            variant = MonarchButtonVariant.Ghost,
            size = MonarchButtonSize.Sm,
            enabled = enabled,
        )
    } else if (allowManualEntry) {
        MonarchButton(
            text = monarchString(labels.enterManually, R.string.monarch_addressautocomplete_enter_manually),
            onClick = { setMode(MonarchAddressAutocompleteMode.Manual) },
            variant = MonarchButtonVariant.Ghost,
            size = MonarchButtonSize.Sm,
            enabled = enabled,
        )
    }
}

/**
 * The search field: a read-only Monarch field box with the label above it that
 * shows the address on one line or the placeholder, and opens the sheet. It
 * has the drop-down role, so TalkBack reads it as a control that expands, and
 * its state is the whole address.
 */
@Composable
@Suppress("LongParameterList")
private fun AddressAutocompleteSearchField(
    summary: String?,
    label: String,
    placeholder: String,
    helperText: String?,
    errorText: String?,
    enabled: Boolean,
    required: Boolean,
    open: Boolean,
    onOpen: () -> Unit,
    searchIcon: (@Composable () -> Unit)?,
) {
    val shown = summary ?: placeholder
    MonarchFieldFrame(
        label = label,
        required = required,
        enabled = enabled,
        helperText = helperText,
        errorText = errorText,
    ) {
        MonarchFieldTrigger(
            text = shown,
            isPlaceholder = summary == null,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, role = Role.DropdownList, onClick = onOpen)
                .semantics(mergeDescendants = true) { stateDescription = shown }
                .monarchFieldSemantics(label, errorText),
            expanded = open,
            enabled = enabled,
            invalid = !errorText.isNullOrBlank(),
            leading = searchIcon,
        )
    }
}

/** The labels of the form, or their replacements. */
@Composable
private fun addressAutocompleteFormTexts(labels: MonarchAddressAutocompleteLabels): AddressAutocompleteFormTexts =
    AddressAutocompleteFormTexts(
        line1 = monarchString(labels.line1, R.string.monarch_addressautocomplete_line1),
        line2 = monarchString(labels.line2, R.string.monarch_addressautocomplete_line2),
        city = monarchString(labels.city, R.string.monarch_addressautocomplete_city),
        region = monarchString(labels.region, R.string.monarch_addressautocomplete_region),
        postalCode = monarchString(labels.postalCode, R.string.monarch_addressautocomplete_postal_code),
        country = monarchString(labels.country, R.string.monarch_addressautocomplete_country),
        optional = monarchString(labels.optional, R.string.monarch_addressautocomplete_optional),
    )

/** The texts of the sheet, or their replacements. */
@Composable
private fun addressAutocompleteSheetTexts(
    labels: MonarchAddressAutocompleteLabels,
    title: String,
    minQueryLength: Int,
): AddressAutocompleteSheetTexts {
    val tooShort = pluralStringResource(R.plurals.monarch_addressautocomplete_too_short, minQueryLength, minQueryLength)
    val resources = LocalResources.current
    return AddressAutocompleteSheetTexts(
        title = title,
        placeholder = monarchString(labels.placeholder, R.string.monarch_addressautocomplete_placeholder),
        searching = monarchString(labels.searching, R.string.monarch_addressautocomplete_searching),
        empty = monarchString(labels.empty, R.string.monarch_addressautocomplete_empty),
        searchFailed = monarchString(labels.searchFailed, R.string.monarch_addressautocomplete_search_failed),
        enterManually = monarchString(labels.enterManually, R.string.monarch_addressautocomplete_enter_manually),
        cancel = monarchString(labels.cancel, R.string.monarch_addressautocomplete_cancel),
        tooShort = labels.tooShort?.invoke(minQueryLength) ?: tooShort,
        results = labels.results ?: { count ->
            resources.getQuantityString(R.plurals.monarch_addressautocomplete_results, count, count)
        },
    )
}

/**
 * Fills the address from a picked suggestion: its own address, the street line alone without [resolve], or what
 * [resolve] returns, run through [launch]. The work given to [launch] returns whether resolving failed.
 */
private fun addressAutocompletePick(
    suggestion: MonarchAddressAutocompleteSuggestion,
    resolve: (suspend (MonarchAddressAutocompleteSuggestion) -> MonarchAddressAutocompleteAddress)?,
    fill: (MonarchAddressAutocompleteSuggestion, MonarchAddressAutocompleteAddress) -> Unit,
    launch: (suspend () -> Boolean) -> Unit,
) {
    val known = suggestion.address
    when {
        known != null -> fill(suggestion, known)
        resolve == null -> fill(suggestion, MonarchAddressAutocompleteAddress(line1 = suggestion.label))
        else -> launch {
            try {
                fill(suggestion, resolve(suggestion))
                false
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (@Suppress("TooGenericExceptionCaught", "SwallowedException") failed: Exception) {
                // A failed resolve is shown on the field and announced, not thrown.
                true
            }
        }
    }
}
