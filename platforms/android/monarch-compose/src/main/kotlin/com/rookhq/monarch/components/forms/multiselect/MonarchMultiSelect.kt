package com.rookhq.monarch.components.forms.multiselect

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.rookhq.monarch.R
import com.rookhq.monarch.components.datadisplay.tag.MonarchTag
import com.rookhq.monarch.components.forms.combobox.ComboboxFilter
import com.rookhq.monarch.components.forms.combobox.ComboboxRules
import com.rookhq.monarch.components.forms.combobox.MonarchComboboxOption
import com.rookhq.monarch.components.forms.formfield.MonarchFormField
import com.rookhq.monarch.internal.MonarchDefaultIcons
import com.rookhq.monarch.internal.field.MonarchFieldBox
import com.rookhq.monarch.internal.field.MonarchFieldPlaceholder
import com.rookhq.monarch.internal.field.rememberFieldInteractionSource
import com.rookhq.monarch.internal.field.rememberMonarchFieldState
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme

/**
 * Chooses several values from a long list: the chosen values are removable
 * tags above a button that opens a searchable checklist in a bottom sheet.
 *
 * Built from Monarch and Material 3 parts: `MonarchFormField` for the label,
 * help and error, `MonarchTag` (Material 3 `AssistChip`) for each chosen
 * value, `MonarchButton` to open the list, and a Material 3
 * `ModalBottomSheet` with `MonarchSearchBar` and a `LazyColumn` of rows that
 * carry a `Checkbox` mark. The list is searched like `MonarchCombobox`: the
 * label, hint and keywords, ignoring case and accents, under group headings.
 * The sheet stays open between picks until Done. With [maxSelections], other
 * rows are disabled at the limit and a count shows under the field and in the
 * sheet.
 *
 * For one value use `MonarchCombobox`; for free text values use
 * `MonarchTagInput`; for a few fixed options use `MonarchCheckboxGroup`.
 *
 * @param label the visible label of the field, and the title of the sheet. Already localised.
 * @param options the options in order.
 * @param selected the ids of the chosen options, in the order they were picked.
 * @param onSelectedChange called with every chosen id after each pick or removal.
 * @param modifier the modifier of the field.
 * @param enabled whether the selection can be changed.
 * @param required shows a mark after the label and says "required" with the field's name.
 * @param loading options are being fetched: the list shows "Loading…" while nothing matches.
 * @param sortByMatch while searching, lists labels that start with the text first, then labels with a word that
 * does.
 * @param placeholder the button's text while nothing is chosen. `null` uses "Select…".
 * @param helperText guidance below the field.
 * @param errorText the error message. TalkBack reads it with the button and it is announced when it appears.
 * @param maxSelections the most values that can be chosen.
 * @param limit shows at most this many matches, with a note to keep typing.
 * @param query the search text, for apps that search a server: pass it with [onQueryChange] and a [filter] that
 * keeps every option. `null` keeps the text inside.
 * @param onQueryChange called with the search text on every keystroke, and with "" when the sheet closes.
 * @param filter decides whether an option matches the trimmed search text. `null` searches the label, hint and
 * keywords.
 * @param onCreate offers "Create “text”" when no label equals the typed text and the limit is not reached. Return
 * the new option to choose it.
 * @param removeIcon the icon of each tag's remove button. Monarch bundles no icons; without it the tags cannot be
 * removed in place and values are removed in the sheet.
 * @param labels replacements for the built-in strings.
 */
@Composable
@Suppress("LongParameterList", "LongMethod")
public fun MonarchMultiSelect(
    label: String,
    options: List<MonarchComboboxOption>,
    selected: List<String>,
    onSelectedChange: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    required: Boolean = false,
    loading: Boolean = false,
    sortByMatch: Boolean = false,
    placeholder: String? = null,
    helperText: String? = null,
    errorText: String? = null,
    maxSelections: Int? = null,
    limit: Int? = null,
    query: String? = null,
    onQueryChange: ((String) -> Unit)? = null,
    filter: ((MonarchComboboxOption, String) -> Boolean)? = null,
    onCreate: ((String) -> MonarchComboboxOption?)? = null,
    removeIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.Close,
    labels: MonarchMultiSelectLabels = MonarchMultiSelectLabels(),
) {
    val tokens = MonarchMultiSelectTokens
    var expanded by rememberSaveable { mutableStateOf(false) }
    var ownQuery by rememberSaveable { mutableStateOf("") }
    val text = query ?: ownQuery
    val setQuery: (String) -> Unit = {
        ownQuery = it
        onQueryChange?.invoke(it)
    }
    val atLimit = tokens.atLimit(selected, maxSelections)
    val count = multiSelectCount(selected.size, maxSelections, labels.count)
    val chosen = tokens.chosenLabels(selected, options)
    val none = monarchString(labels.none, R.string.monarch_multiselect_none)
    val buttonText = multiSelectButtonText(selected.isEmpty(), placeholder, labels)
    MonarchFormField(
        label = label,
        modifier = modifier,
        enabled = enabled,
        required = required,
        helperText = helperText,
        errorText = errorText,
    ) { controlModifier ->
        Column(verticalArrangement = Arrangement.spacedBy(tokens.Gap)) {
            // One field box as the web draws it: the chosen values as tags, or the prompt, and a chevron.
            val interaction = rememberFieldInteractionSource()
            MonarchFieldBox(
                modifier = controlModifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        enabled = enabled,
                        onClickLabel = buttonText,
                        role = Role.Button,
                    ) { expanded = true }
                    .semantics {
                        stateDescription = if (chosen.isEmpty()) none else chosen.joinToString(", ") { it.second }
                    },
                state = rememberMonarchFieldState(interaction, enabled, invalid = errorText != null),
                trailing = { MonarchDefaultIcons.ChevronDown() },
            ) {
                if (chosen.isEmpty()) {
                    MonarchFieldPlaceholder(buttonText, enabled)
                } else {
                    MultiSelectTags(chosen, enabled, removeIcon, labels.remove) { id ->
                        onSelectedChange(selected - id)
                    }
                }
            }
            if (count != null) {
                Text(
                    text = count,
                    style = MonarchTheme.typography.caption,
                    color = tokens.secondaryColor(enabled, MonarchTheme.colors),
                )
            }
        }
    }
    if (expanded && enabled) {
        val results = ComboboxFilter.results(
            options,
            text,
            ComboboxRules(filter, sortByMatch, limit, canCreate = onCreate != null && !atLimit),
        )
        MultiSelectSheet(
            title = label,
            query = text,
            loading = loading,
            entries = multiSelectEntries(results, text, loading, labels.list),
            selected = selected,
            atLimit = atLimit,
            count = count,
            texts = MultiSelectSheetTexts(
                search = monarchString(labels.list.search, R.string.monarch_combobox_search),
                done = monarchString(labels.done, R.string.monarch_multiselect_done),
            ),
            onQueryChange = setQuery,
            onToggle = { id -> onSelectedChange(tokens.toggle(selected, id, maxSelections)) },
            onCreate = { typed ->
                onCreate?.invoke(typed)?.let { created ->
                    if (created.id !in selected) onSelectedChange(tokens.toggle(selected, created.id, maxSelections))
                }
                setQuery("")
            },
            onDone = {
                expanded = false
                if (text.isNotEmpty()) setQuery("")
            },
        )
    }
}

/** The chosen values as tags, removable when there is an icon. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MultiSelectTags(
    chosen: List<Pair<String, String>>,
    enabled: Boolean,
    removeIcon: (@Composable () -> Unit)?,
    removeLabel: ((String) -> String)?,
    onRemove: (String) -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    val removeFormat = stringResource(R.string.monarch_multiselect_remove)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(MonarchMultiSelectTokens.Gap),
        verticalArrangement = Arrangement.spacedBy(MonarchMultiSelectTokens.Gap),
    ) {
        chosen.forEach { (id, name) ->
            if (removeIcon != null) {
                MonarchTag(
                    text = name,
                    onRemove = { onRemove(id) },
                    removeIcon = removeIcon,
                    enabled = enabled,
                    removeLabel = removeLabel?.invoke(name) ?: String.format(locale, removeFormat, name),
                )
            } else {
                MonarchTag(text = name)
            }
        }
    }
}

/** The count under the field when there is a limit: "2 of 3 selected". */
@Composable
private fun multiSelectCount(count: Int, maxSelections: Int?, format: ((Int, Int) -> String)?): String? {
    val max = maxSelections ?: return null
    return format?.invoke(count, max) ?: pluralStringResource(R.plurals.monarch_multiselect_count, max, count, max)
}

/** The button's text: the prompt while nothing is chosen, then "Edit selection". */
@Composable
private fun multiSelectButtonText(empty: Boolean, placeholder: String?, labels: MonarchMultiSelectLabels): String =
    if (empty) {
        placeholder ?: monarchString(labels.select, R.string.monarch_multiselect_select)
    } else {
        monarchString(labels.change, R.string.monarch_multiselect_change)
    }
