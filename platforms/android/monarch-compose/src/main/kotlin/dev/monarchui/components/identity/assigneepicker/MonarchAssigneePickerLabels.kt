package dev.monarchui.components.identity.assigneepicker

import androidx.compose.runtime.Immutable
import dev.monarchui.components.forms.entitypicker.MonarchEntityPickerLabels

/**
 * Replacements for the built-in strings of `MonarchAssigneePicker`. Every
 * `null` keeps the built-in text.
 *
 * @property label the label of the field and the title of the sheet. Default "Assignee".
 * @property unassigned the row that clears the choice, and the field while nobody is assigned. Default "Unassigned".
 * @property you the current user's name in the list. Default "Name (you)".
 * @property assignToMe the shortcut under the field. Default "Assign to me".
 * @property count the field with several people assigned. Default "3 people".
 * @property search the placeholder of the search field. Default "Assign to…".
 * @property picker replacements for the strings of the underlying `MonarchEntityPicker`.
 */
@Immutable
public class MonarchAssigneePickerLabels(
    public val label: String? = null,
    public val unassigned: String? = null,
    public val you: ((name: String) -> String)? = null,
    public val assignToMe: String? = null,
    public val count: ((count: Int) -> String)? = null,
    public val search: String? = null,
    public val picker: MonarchEntityPickerLabels = MonarchEntityPickerLabels(),
)
