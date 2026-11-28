package com.rookhq.monarch.components.forms.checkboxrow

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rookhq.monarch.components.forms.checkbox.MonarchCheckbox

/**
 * A checkbox with a label, as one row that toggles as a whole.
 *
 * The box is the Monarch [MonarchCheckbox] (16dp, accent fill with a check mark). The row adds what a bare
 * checkbox lacks: a label that wraps, a target as wide as the row and at least 48dp high, and one TalkBack
 * element that reads the label, the role and the state ("checked" or "not checked"). For a checkbox without a
 * visible label, use `MonarchCheckbox` directly.
 *
 * @param checked whether the box is ticked.
 * @param onCheckedChange called with the new value when the user toggles the row.
 * @param label the visible label.
 * @param modifier the modifier of the row.
 * @param enabled whether the user can toggle the row.
 * @param description secondary text below the label.
 */
@Composable
public fun MonarchCheckboxRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    description: String? = null,
) {
    MonarchCheckbox(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        label = label,
        description = description,
    )
}
