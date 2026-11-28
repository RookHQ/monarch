package dev.monarchui.components.forms.select

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import dev.monarchui.components.forms.textfield.MonarchFieldGlyph
import dev.monarchui.components.forms.textfield.MonarchFieldGlyphIcon
import dev.monarchui.theme.MonarchTheme

/**
 * One row of the menu of a select or a combobox: the platform's `DropdownMenuItem` (48dp, focus, keyboard
 * and TalkBack behaviour) with Monarch's look: a `bgMuted` fill when chosen and an accent check at the end.
 * The row carries the selected state; the check is decoration.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MonarchMenuRow(
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    mark: Boolean,
    leading: (@Composable () -> Unit)? = null,
    hint: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MonarchTheme.colors
    DropdownMenuItem(
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(MonarchSelectTokens.ItemTextGap), content = content)
        },
        onClick = onClick,
        modifier = Modifier
            .background(if (selected) colors.bgMuted else colors.bgRaised)
            .semantics { this.selected = selected },
        leadingIcon = leading,
        trailingIcon = if (mark || hint != null) {
            {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(MonarchSelectTokens.ItemTextGap),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    hint?.invoke()
                    if (mark) {
                        MonarchFieldGlyphIcon(
                            MonarchFieldGlyph.Check,
                            tint = if (enabled) colors.accentStrong else colors.textDisabled,
                        )
                    }
                }
            }
        } else {
            null
        },
        enabled = enabled,
        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
    )
}
