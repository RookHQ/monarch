package com.rookhq.monarch.components.forms.entitypicker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.rookhq.monarch.internal.MonarchDefaultIcons

/**
 * Chooses one person, record or other entity from a list the app can search
 * on its server, shown with avatars.
 *
 * Built from Monarch and Material 3 parts: `MonarchFormField` for the label,
 * help and error; a field that shows the chosen entity's `MonarchAvatar` and
 * name; and a Material 3 `ModalBottomSheet` with `MonarchSearchBar` and a
 * `LazyColumn` of avatar rows with a `RadioButton` mark, the pattern of the
 * system contact pickers. Choosing a row closes the sheet.
 *
 * Without [search], [entities] are filtered by name, hint and keywords,
 * ignoring case and accents. With it, [search] runs [debounceMillis] after
 * the last keystroke for text of at least [minSearchLength] characters; a
 * newer text cancels the older search, and a failed search shows Try again.
 *
 * For options that are not people or records use `MonarchCombobox`.
 *
 * @param label the visible label of the field, and the title of the sheet. Already localised.
 * @param entities the entities to pick from. With [search], shown until the first search runs.
 * @param selected the chosen id, or `null`.
 * @param onSelectedChange called with the chosen id, or `null` when the selection is cleared.
 * @param modifier the modifier of the field.
 * @param selectedEntities entities for chosen ids that may not be in [entities], so the field can show their names
 * and pictures.
 * @param enabled whether the field can be opened.
 * @param required shows a mark after the label and says "required" with the field's name.
 * @param loading shows "Loading…" in the list while [entities] is empty.
 * @param placeholder shown in the field while nothing is chosen. `null` uses "Select…".
 * @param helperText guidance below the field.
 * @param errorText the error message. TalkBack reads it with the field and it is announced when it appears.
 * @param clearOptionLabel adds a first row that clears the selection, such as "Unassigned". Hidden while searching.
 * @param minSearchLength the shortest trimmed text [search] runs for.
 * @param debounceMillis how long to wait after typing before [search] runs.
 * @param search loads the entities matching the trimmed text. Throw to show the failure with Try again.
 * @param onCreate offers "Create “text”" when no shown name equals the typed text. The returned entity is chosen;
 * `null` chooses nothing.
 * @param clearIcon the icon of a clear button in the field, shown while something is chosen. Monarch bundles no
 * icons; without it there is no clear button.
 * @param labels replacements for the built-in strings.
 * @param initiallyOpen opens the list when the field first appears, as the web example does.
 * @param footerLabel the label of an action at the bottom of the list, such as "Manage projects". Needs [onFooter].
 * @param onFooter called when the footer action is tapped; the list closes first.
 */
@Composable
@Suppress("LongParameterList")
public fun MonarchEntityPicker(
    label: String,
    entities: List<MonarchEntityPickerEntity>,
    selected: String?,
    onSelectedChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
    selectedEntities: List<MonarchEntityPickerEntity> = emptyList(),
    enabled: Boolean = true,
    required: Boolean = false,
    loading: Boolean = false,
    placeholder: String? = null,
    helperText: String? = null,
    errorText: String? = null,
    clearOptionLabel: String? = null,
    minSearchLength: Int = 0,
    debounceMillis: Long = 250L,
    search: (suspend (String) -> List<MonarchEntityPickerEntity>)? = null,
    onCreate: (suspend (String) -> MonarchEntityPickerEntity?)? = null,
    clearIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.Close,
    labels: MonarchEntityPickerLabels = MonarchEntityPickerLabels(),
    initiallyOpen: Boolean = false,
    footerLabel: String? = null,
    onFooter: (() -> Unit)? = null,
) {
    val known = remember { mutableStateMapOf<String, MonarchEntityPickerEntity>() }
    EntityPickerField(
        config = EntityPickerConfig(
            label = label,
            entities = entities,
            selected = listOfNotNull(selected),
            multiple = false,
            selectedEntities = selectedEntities,
            enabled = enabled,
            required = required,
            loading = loading,
            placeholder = placeholder,
            helperText = helperText,
            errorText = errorText,
            clearOptionLabel = clearOptionLabel,
            minSearchLength = minSearchLength,
            debounceMillis = debounceMillis,
            search = search,
            onCreate = onCreate,
            clearIcon = clearIcon,
            labels = labels,
            initiallyOpen = initiallyOpen,
            footerLabel = footerLabel,
            onFooter = onFooter,
        ),
        known = known,
        onChange = { onSelectedChange(it.lastOrNull()) },
        modifier = modifier,
    )
}

/**
 * Chooses several people, records or other entities from a list the app can
 * search on its server, shown with avatars.
 *
 * The same field and sheet as the single-choice `MonarchEntityPicker`, with a
 * `Checkbox` mark on each row: the sheet stays open between picks until Done,
 * and the field shows the first chosen name and how many more ("Ada Lovelace
 * +2"). For values that are not people or records use `MonarchMultiSelect`.
 *
 * @param label the visible label of the field, and the title of the sheet. Already localised.
 * @param entities the entities to pick from. With [search], shown until the first search runs.
 * @param selected the chosen ids, in the order they were picked.
 * @param onSelectedChange called with every chosen id after each pick or removal.
 * @param modifier the modifier of the field.
 * @param selectedEntities entities for chosen ids that may not be in [entities], so the field can show their names
 * and pictures.
 * @param enabled whether the field can be opened.
 * @param required shows a mark after the label and says "required" with the field's name.
 * @param loading shows "Loading…" in the list while [entities] is empty.
 * @param placeholder shown in the field while nothing is chosen. `null` uses "Select…".
 * @param helperText guidance below the field.
 * @param errorText the error message. TalkBack reads it with the field and it is announced when it appears.
 * @param minSearchLength the shortest trimmed text [search] runs for.
 * @param debounceMillis how long to wait after typing before [search] runs.
 * @param search loads the entities matching the trimmed text. Throw to show the failure with Try again.
 * @param onCreate offers "Create “text”" when no shown name equals the typed text. The returned entity is added to
 * the selection; `null` adds nothing.
 * @param clearIcon the icon of a clear button in the field, shown while something is chosen. Monarch bundles no
 * icons; without it there is no clear button.
 * @param labels replacements for the built-in strings.
 * @param initiallyOpen opens the list when the field first appears, as the web example does.
 */
@Composable
@Suppress("LongParameterList")
public fun MonarchEntityPicker(
    label: String,
    entities: List<MonarchEntityPickerEntity>,
    selected: List<String>,
    onSelectedChange: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
    selectedEntities: List<MonarchEntityPickerEntity> = emptyList(),
    enabled: Boolean = true,
    required: Boolean = false,
    loading: Boolean = false,
    placeholder: String? = null,
    helperText: String? = null,
    errorText: String? = null,
    minSearchLength: Int = 0,
    debounceMillis: Long = 250L,
    search: (suspend (String) -> List<MonarchEntityPickerEntity>)? = null,
    onCreate: (suspend (String) -> MonarchEntityPickerEntity?)? = null,
    clearIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.Close,
    labels: MonarchEntityPickerLabels = MonarchEntityPickerLabels(),
    initiallyOpen: Boolean = false,
) {
    val known = remember { mutableStateMapOf<String, MonarchEntityPickerEntity>() }
    EntityPickerField(
        config = EntityPickerConfig(
            label = label,
            entities = entities,
            selected = selected,
            multiple = true,
            selectedEntities = selectedEntities,
            enabled = enabled,
            required = required,
            loading = loading,
            placeholder = placeholder,
            helperText = helperText,
            errorText = errorText,
            clearOptionLabel = null,
            minSearchLength = minSearchLength,
            debounceMillis = debounceMillis,
            search = search,
            onCreate = onCreate,
            clearIcon = clearIcon,
            labels = labels,
            initiallyOpen = initiallyOpen,
        ),
        known = known,
        onChange = onSelectedChange,
        modifier = modifier,
    )
}
