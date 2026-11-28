package com.rookhq.monarch.internal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.rookhq.monarch.theme.MonarchTheme

/**
 * A Material 3 control beside a label with an optional description, at least
 * 48dp high. The caller's [modifier] carries the `toggleable` or `selectable`
 * behavior, so the whole row is one target and one TalkBack element.
 *
 * @param label the visible label.
 * @param description secondary text below the label.
 * @param enabled whether the row is enabled; a disabled row uses the disabled text colour.
 * @param controlAtEnd `true` puts the control after the label (a switch), `false` before it (a checkbox).
 * @param modifier the modifier of the row, including the interaction.
 * @param control the control, without its own click handling.
 */
@Composable
internal fun LabelledControlRow(
    label: String,
    description: String?,
    enabled: Boolean,
    controlAtEnd: Boolean,
    modifier: Modifier,
    control: @Composable () -> Unit,
) {
    val colors = MonarchTheme.colors
    Row(
        modifier = modifier
            .heightIn(min = LabelledControlRowTokens.MinHeight)
            .padding(vertical = LabelledControlRowTokens.VerticalPadding),
        horizontalArrangement = Arrangement.spacedBy(LabelledControlRowTokens.Gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!controlAtEnd) control()
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(LabelledControlRowTokens.DescriptionGap),
        ) {
            Text(
                text = label,
                style = MonarchTheme.typography.body,
                color = if (enabled) colors.textDefault else colors.textDisabled,
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MonarchTheme.typography.caption,
                    color = if (enabled) colors.textMuted else colors.textDisabled,
                )
            }
        }
        if (controlAtEnd) control()
    }
}
