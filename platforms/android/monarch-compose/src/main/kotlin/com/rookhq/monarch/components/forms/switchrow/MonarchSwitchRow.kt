package com.rookhq.monarch.components.forms.switchrow

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rookhq.monarch.components.forms.switch.MonarchSwitch

/**
 * A switch with a label, as one row that toggles as a whole.
 *
 * The label is at the start and the switch at the end, as in the system settings. The switch is the Monarch
 * [MonarchSwitch]: the Material 52 by 32dp track in Monarch colours. Use a switch for a setting that takes
 * effect immediately, and `MonarchCheckboxRow` for a choice that is submitted with a form.
 *
 * @param checked whether the switch is on.
 * @param onCheckedChange called with the new value when the user toggles the row.
 * @param label the visible label.
 * @param modifier the modifier of the row.
 * @param enabled whether the user can toggle the row.
 * @param description secondary text below the label.
 */
@Composable
public fun MonarchSwitchRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    description: String? = null,
) {
    MonarchSwitch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        label = label,
        description = description,
    )
}
