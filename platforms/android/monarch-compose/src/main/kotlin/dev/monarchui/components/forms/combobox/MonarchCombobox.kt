package dev.monarchui.components.forms.combobox

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import dev.monarchui.R
import dev.monarchui.internal.monarchString

/**
 * Chooses one option from a list the person can search: an editable field
 * over a filtered menu for short lists, or a field that opens a bottom sheet
 * with a search field for long ones.
 *
 * Built on Material 3: the menu presentation is an `ExposedDropdownMenuBox`
 * with an editable `OutlinedTextField` anchor that filters an
 * `ExposedDropdownMenu`; the sheet presentation is an `ExposedDropdownMenuBox`
 * with a read-only anchor that opens a `ModalBottomSheet` holding a
 * `MonarchSearchBar` and a `LazyColumn` of selectable rows. Both list the
 * options under their group headings with descriptions and hints, keep
 * disabled options visible, and mark the chosen option with a radio mark
 * that TalkBack reads as selected. By default the label, hint and keywords
 * are searched, ignoring case and accents. An optional first row clears the
 * selection; [onCreate] adds a "Create “text”" row when no label equals the
 * typed text; [limit] cuts long result lists with a note.
 *
 * For a short, fixed list without search, use `MonarchSelect`. For several
 * values, use `MonarchTagInput` or `MonarchCheckboxGroup`.
 *
 * @param label the visible label of the field, and the title of the sheet. Already localised.
 * @param options the options in order.
 * @param selected the id of the chosen option, or `null`.
 * @param onSelectedChange called with the id of the chosen option, or `null` when the selection is cleared.
 * @param modifier the modifier of the field.
 * @param presentation a menu, a sheet, or [MonarchComboboxPresentation.Automatic]: a menu for up to eight options
 * that the app does not search, a sheet otherwise.
 * @param enabled whether the list can be opened.
 * @param loading options are being fetched: the list shows "Loading…" while nothing matches, and the sheet's
 * search field shows its pending state.
 * @param sortByMatch while searching, lists labels that start with the text first, then labels with a word that
 * does, then the rest, keeping the order within each.
 * @param placeholder shown in the empty field while nothing is chosen. `null` uses "Search" in the menu
 * presentation.
 * @param helperText guidance below the field.
 * @param errorText the error message. The field is in the error state and TalkBack reads it with the field.
 * @param clearOptionLabel adds a first row that clears the selection, such as "Unassigned". Hidden while searching.
 * @param limit shows at most this many matches, with "Showing 50 of 120. Type to narrow the list.".
 * @param query the search text, for apps that search a server: pass it with [onQueryChange], a [filter] that keeps
 * every option, and update [options] and [loading] from it. `null` keeps the text inside the combobox.
 * @param onQueryChange called with the search text on every keystroke, and with "" when the list closes.
 * @param filter decides whether an option matches the trimmed search text. `null` searches the label, hint and
 * keywords.
 * @param onCreate offers "Create “text”" when no label equals the typed text. Return the new option to choose it,
 * or `null` to only close the list.
 * @param clearIcon the icon of a clear button at the end of the field, shown while something is chosen. Monarch
 * bundles no icons; without it there is no clear button (use [clearOptionLabel] instead).
 * @param labels replacements for the built-in strings.
 * @param loadError the options could not be loaded: the list shows this message (an empty string uses the
 * built-in "The options could not be loaded.") and, with [onRetry], a "Try again" button. `null` when there is no
 * error.
 * @param onRetry loads the options again after [loadError]. The list stays open.
 * @param initiallyOpen opens the list when the field first appears, as the web example does.
 * @param labelVisible whether the label shows above the field; false keeps it as the spoken name, for a field in a
 * row that has its own label.
 * @param required shows a mark after the label.
 */
@Composable
@Suppress("LongParameterList")
public fun MonarchCombobox(
    label: String,
    options: List<MonarchComboboxOption>,
    selected: String?,
    onSelectedChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
    presentation: MonarchComboboxPresentation = MonarchComboboxPresentation.Automatic,
    enabled: Boolean = true,
    loading: Boolean = false,
    sortByMatch: Boolean = false,
    placeholder: String? = null,
    helperText: String? = null,
    errorText: String? = null,
    clearOptionLabel: String? = null,
    limit: Int? = null,
    query: String? = null,
    onQueryChange: ((String) -> Unit)? = null,
    filter: ((MonarchComboboxOption, String) -> Boolean)? = null,
    onCreate: ((String) -> MonarchComboboxOption?)? = null,
    clearIcon: (@Composable () -> Unit)? = null,
    labels: MonarchComboboxLabels = MonarchComboboxLabels(),
    loadError: String? = null,
    onRetry: (() -> Unit)? = null,
    initiallyOpen: Boolean = false,
    labelVisible: Boolean = true,
    required: Boolean = false,
) {
    var expanded by rememberSaveable { mutableStateOf(initiallyOpen) }
    var ownQuery by rememberSaveable { mutableStateOf("") }
    val text = query ?: ownQuery
    val setQuery: (String) -> Unit = {
        ownQuery = it
        onQueryChange?.invoke(it)
    }
    val results = ComboboxFilter.results(options, text, ComboboxRules(filter, sortByMatch, limit, onCreate != null))
    val close = {
        expanded = false
        if (text.isNotEmpty()) setQuery("")
    }
    val state = ComboboxState(
        labelVisible = labelVisible,
        required = required,
        label = label,
        chosen = options.firstOrNull { it.id == selected },
        selected = selected,
        query = text,
        expanded = expanded && enabled,
        enabled = enabled,
        loading = loading,
        entries = ComboboxEntries.build(
            results = results,
            query = text,
            clearOptionLabel = clearOptionLabel,
            loading = loading,
            messages = comboboxMessages(labels),
            failure = loadError?.let {
                ComboboxFailure(
                    message = it.ifEmpty { monarchString(labels.failed, R.string.monarch_combobox_failed) },
                    canRetry = onRetry != null,
                )
            },
        ),
        texts = ComboboxTexts(
            placeholder = placeholder,
            helperText = helperText,
            errorText = errorText,
            search = monarchString(labels.search, R.string.monarch_combobox_search),
            clearName = monarchString(labels.clear, R.string.monarch_combobox_clear),
            cancel = monarchString(labels.cancel, R.string.monarch_combobox_cancel),
        ),
    )
    val actions = ComboboxActions(
        open = { if (enabled) expanded = true },
        close = close,
        type = { typed ->
            setQuery(typed)
            if (enabled) expanded = true
        },
        choose = { entry ->
            if (entry is ComboboxEntry.Message && entry.failed) {
                onRetry?.invoke()
            } else {
                comboboxChoose(entry, onSelectedChange, onCreate)
                close()
            }
        },
        clear = { onSelectedChange(null) }.takeIf { clearIcon != null && selected != null && enabled },
        clearIcon = clearIcon,
    )
    when (MonarchComboboxTokens.presentation(presentation, options.size, onQueryChange != null)) {
        MonarchComboboxPresentation.Sheet -> ComboboxSheetField(state, actions, modifier)
        else -> ComboboxMenuField(state, actions, modifier)
    }
}

/** Applies a chosen row: an option's id, `null` for the clear row, or the created option's id. */
private fun comboboxChoose(
    entry: ComboboxEntry,
    onSelectedChange: (String?) -> Unit,
    onCreate: ((String) -> MonarchComboboxOption?)?,
) {
    when (entry) {
        is ComboboxEntry.Clear -> onSelectedChange(null)
        is ComboboxEntry.Option -> onSelectedChange(entry.option.id)
        is ComboboxEntry.Create -> onCreate?.invoke(entry.text)?.let { onSelectedChange(it.id) }
        else -> Unit
    }
}

/** The built-in messages of the list, or their replacements. */
@Composable
private fun comboboxMessages(labels: MonarchComboboxLabels): ComboboxMessages {
    val locale = LocalConfiguration.current.locales[0]
    val create = stringResource(R.string.monarch_combobox_create)
    val more = stringResource(R.string.monarch_combobox_more)
    return ComboboxMessages(
        loading = monarchString(labels.loading, R.string.monarch_combobox_loading),
        empty = monarchString(labels.empty, R.string.monarch_combobox_empty),
        create = labels.create ?: { String.format(locale, create, it) },
        more = labels.more ?: { shown, total -> String.format(locale, more, shown, total) },
        retry = monarchString(labels.retry, R.string.monarch_combobox_retry),
    )
}
