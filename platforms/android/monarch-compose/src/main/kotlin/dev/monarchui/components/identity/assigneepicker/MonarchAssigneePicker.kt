package dev.monarchui.components.identity.assigneepicker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.forms.entitypicker.MonarchEntityPicker
import dev.monarchui.components.forms.entitypicker.MonarchEntityPickerEntity
import dev.monarchui.components.forms.entitypicker.MonarchEntityPickerLabels
import dev.monarchui.internal.monarchString

/**
 * Assigns one person from a searchable list with avatars: the current user
 * first as "Name (you)", an "Unassigned" row that clears the choice, and an
 * "Assign to me" shortcut under the field.
 *
 * A preset of `MonarchEntityPicker`, so it is a field that opens a Material 3
 * `ModalBottomSheet` with a search field and avatar rows, the pattern of the
 * system contact pickers. The shortcut is a `MonarchButton` under the field,
 * shown while a current user is known and not assigned; it assigns them
 * without opening the sheet. Without [search] the people are filtered by
 * name and email; with it the app searches its server.
 *
 * @param people the people to choose from, such as the workspace's members.
 * @param selected the id of the assigned person, or `null` for unassigned.
 * @param onSelectedChange called with the chosen id, or `null` when Unassigned is chosen.
 * @param modifier the modifier of the column holding the field and the shortcut.
 * @param selectedPeople people for [selected] that may not be in [people], for their name and avatar.
 * @param currentUserId the signed-in user: listed first with "(you)", and the target of Assign to me.
 * @param enabled whether the field and the shortcut can be used.
 * @param required shows a mark after the label and says "required" with the field's name.
 * @param loading shows "Loading…" in the list while [people] is empty.
 * @param allowUnassigned offers the Unassigned row and shows "Unassigned" in the empty field.
 * @param showAssignToMe shows the Assign to me shortcut when [currentUserId] is set.
 * @param helperText guidance below the field.
 * @param errorText the error message. TalkBack reads it with the field.
 * @param search loads the people matching the trimmed text from the app's server.
 * @param labels replacements for the built-in strings.
 * @param initiallyOpen opens the list when the field first appears, as the web example does.
 */
@Composable
@Suppress("LongParameterList")
public fun MonarchAssigneePicker(
    people: List<MonarchAssigneePerson>,
    selected: String?,
    onSelectedChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
    selectedPeople: List<MonarchAssigneePerson> = emptyList(),
    currentUserId: String? = null,
    enabled: Boolean = true,
    required: Boolean = false,
    loading: Boolean = false,
    allowUnassigned: Boolean = true,
    showAssignToMe: Boolean = true,
    helperText: String? = null,
    errorText: String? = null,
    search: (suspend (String) -> List<MonarchAssigneePerson>)? = null,
    labels: MonarchAssigneePickerLabels = MonarchAssigneePickerLabels(),
    initiallyOpen: Boolean = false,
) {
    val texts = assigneePickerTexts(labels)
    val tokens = MonarchAssigneePickerTokens
    val toEntity = { person: MonarchAssigneePerson -> tokens.entity(person, currentUserId, texts.you) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(tokens.Gap)) {
        MonarchEntityPicker(
            label = texts.label,
            entities = tokens.meFirst(people, currentUserId).map(toEntity),
            selected = selected,
            onSelectedChange = onSelectedChange,
            modifier = Modifier.fillMaxWidth(),
            selectedEntities = selectedPeople.map(toEntity),
            enabled = enabled,
            required = required,
            loading = loading,
            placeholder = texts.unassigned.takeIf { allowUnassigned },
            helperText = helperText,
            errorText = errorText,
            clearOptionLabel = texts.unassigned.takeIf { allowUnassigned },
            search = search?.let { load -> { text -> tokens.meFirst(load(text), currentUserId).map(toEntity) } },
            labels = texts.picker,
            initiallyOpen = initiallyOpen,
        )
        if (tokens.offersAssignToMe(currentUserId, showAssignToMe, enabled, listOfNotNull(selected))) {
            MonarchButton(
                text = texts.assignToMe,
                onClick = { onSelectedChange(currentUserId) },
                variant = MonarchButtonVariant.Ghost,
                size = MonarchButtonSize.Sm,
            )
        }
    }
}

/**
 * Assigns several people from a searchable list with avatars: the current
 * user first as "Name (you)" and an "Assign to me" shortcut that adds them.
 *
 * The multiple-choice `MonarchEntityPicker`: the sheet stays open between
 * picks until Done, and the field reads "3 people" once more than one is
 * assigned.
 *
 * @param people the people to choose from, such as the workspace's members.
 * @param selected the ids of the assigned people, in the order they were picked.
 * @param onSelectedChange called with every assigned id after each change.
 * @param modifier the modifier of the column holding the field and the shortcut.
 * @param selectedPeople people for [selected] that may not be in [people], for their names and avatars.
 * @param currentUserId the signed-in user: listed first with "(you)", and added by Assign to me.
 * @param enabled whether the field and the shortcut can be used.
 * @param required shows a mark after the label and says "required" with the field's name.
 * @param loading shows "Loading…" in the list while [people] is empty.
 * @param showAssignToMe shows the Assign to me shortcut when [currentUserId] is set.
 * @param helperText guidance below the field.
 * @param errorText the error message. TalkBack reads it with the field.
 * @param search loads the people matching the trimmed text from the app's server.
 * @param labels replacements for the built-in strings.
 */
@Composable
@Suppress("LongParameterList")
public fun MonarchAssigneePicker(
    people: List<MonarchAssigneePerson>,
    selected: List<String>,
    onSelectedChange: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
    selectedPeople: List<MonarchAssigneePerson> = emptyList(),
    currentUserId: String? = null,
    enabled: Boolean = true,
    required: Boolean = false,
    loading: Boolean = false,
    showAssignToMe: Boolean = true,
    helperText: String? = null,
    errorText: String? = null,
    search: (suspend (String) -> List<MonarchAssigneePerson>)? = null,
    labels: MonarchAssigneePickerLabels = MonarchAssigneePickerLabels(),
) {
    val texts = assigneePickerTexts(labels)
    val tokens = MonarchAssigneePickerTokens
    val toEntity = { person: MonarchAssigneePerson -> tokens.entity(person, currentUserId, texts.you) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(tokens.Gap)) {
        MonarchEntityPicker(
            label = texts.label,
            entities = tokens.meFirst(people, currentUserId).map(toEntity),
            selected = selected,
            onSelectedChange = onSelectedChange,
            modifier = Modifier.fillMaxWidth(),
            selectedEntities = selectedPeople.map(toEntity),
            enabled = enabled,
            required = required,
            loading = loading,
            placeholder = texts.unassigned,
            helperText = helperText,
            errorText = errorText,
            search = search?.let { load -> { text -> tokens.meFirst(load(text), currentUserId).map(toEntity) } },
            labels = texts.picker,
        )
        if (currentUserId != null && tokens.offersAssignToMe(currentUserId, showAssignToMe, enabled, selected)) {
            MonarchButton(
                text = texts.assignToMe,
                onClick = { onSelectedChange(selected + currentUserId) },
                variant = MonarchButtonVariant.Ghost,
                size = MonarchButtonSize.Sm,
            )
        }
    }
}

/** The resolved strings of the picker. */
private class AssigneePickerTexts(
    val label: String,
    val unassigned: String,
    val you: (String) -> String,
    val assignToMe: String,
    val picker: MonarchEntityPickerLabels,
)

@Composable
private fun assigneePickerTexts(labels: MonarchAssigneePickerLabels): AssigneePickerTexts {
    val locale = LocalConfiguration.current.locales[0]
    val youFormat = stringResource(R.string.monarch_assigneepicker_you)
    val resources = LocalResources.current
    val count: (Int) -> String = labels.count
        ?: { n -> resources.getQuantityString(R.plurals.monarch_assigneepicker_count, n, n) }
    val base = labels.picker
    return AssigneePickerTexts(
        label = monarchString(labels.label, R.string.monarch_assigneepicker_label),
        unassigned = monarchString(labels.unassigned, R.string.monarch_assigneepicker_unassigned),
        you = labels.you ?: { name -> String.format(locale, youFormat, name) },
        assignToMe = monarchString(labels.assignToMe, R.string.monarch_assigneepicker_assign_to_me),
        picker = MonarchEntityPickerLabels(
            select = base.select,
            search = labels.search ?: base.search ?: stringResource(R.string.monarch_assigneepicker_search),
            cancel = base.cancel,
            done = base.done,
            loading = base.loading,
            empty = base.empty,
            tooShort = base.tooShort,
            failed = base.failed,
            retry = base.retry,
            create = base.create,
            summary = base.summary ?: { _, others -> count(others + 1) },
            clear = base.clear,
            none = base.none ?: monarchString(labels.unassigned, R.string.monarch_assigneepicker_unassigned),
        ),
    )
}
