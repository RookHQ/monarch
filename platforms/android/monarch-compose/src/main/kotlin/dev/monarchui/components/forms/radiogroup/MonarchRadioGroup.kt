package dev.monarchui.components.forms.radiogroup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.monarchui.theme.MonarchTheme

/**
 * The radios of one question, as the web's `role="radiogroup"`: a column that TalkBack reads as a group, so each
 * radio says its position ("1 of 3").
 *
 * @param modifier the modifier of the group.
 * @param label the visible label of the group, 14sp medium, which is also the group's TalkBack name.
 * @param content the radios, usually [MonarchRadio] with a `label`.
 */
@Composable
public fun MonarchRadioGroup(
    modifier: Modifier = Modifier,
    label: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MonarchTheme.colors
    Column(
        modifier = modifier.selectableGroup().then(
            if (label != null) Modifier.semantics { contentDescription = label } else Modifier,
        ),
    ) {
        if (label != null) {
            Text(
                text = label,
                modifier = Modifier.clearAndSetSemantics { },
                style = MonarchTheme.typography.label,
                color = colors.textDefault,
            )
        }
        content()
    }
}
