package com.rookhq.monarch.components.forms.checkboxgroup

import androidx.compose.runtime.Immutable

/**
 * One choice of a `MonarchCheckboxGroup`.
 *
 * @param T the type of the value.
 * @property value the value added to the group's value when the choice is checked. Unique within the group.
 * @property label the visible label, which is also the TalkBack name of the checkbox.
 * @property description secondary text under the label.
 * @property enabled whether the choice can be changed. Select all leaves a disabled choice as it is.
 */
@Immutable
public class MonarchCheckboxGroupOption<out T>(
    public val value: T,
    public val label: String,
    public val description: String? = null,
    public val enabled: Boolean = true,
)
