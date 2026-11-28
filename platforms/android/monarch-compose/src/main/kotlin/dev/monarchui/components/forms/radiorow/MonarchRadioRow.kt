package dev.monarchui.components.forms.radiorow

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.monarchui.components.forms.radio.MonarchRadio

/**
 * A radio button with a label, as one row that selects as a whole.
 *
 * The ring is the Monarch [MonarchRadio] (16dp with a 10dp accent dot). Put the rows of one question in a
 * `MonarchRadioGroup` (or a `Column(Modifier.selectableGroup())`), so that TalkBack announces them as one group
 * ("1 of 3").
 *
 * @param selected whether this option is the selected one.
 * @param onClick called when the user selects the row.
 * @param label the visible label.
 * @param modifier the modifier of the row.
 * @param enabled whether the user can select the row.
 * @param description secondary text below the label.
 */
@Composable
public fun MonarchRadioRow(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    description: String? = null,
) {
    MonarchRadio(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        label = label,
        description = description,
    )
}
