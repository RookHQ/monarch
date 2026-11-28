package dev.monarchui.components.overlays.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.monarchui.internal.MonarchStrokeGlyph
import dev.monarchui.internal.MonarchStrokeGlyphMark
import dev.monarchui.theme.MonarchTheme

/**
 * One row of a [MonarchMenu]: an optional icon, the text, an optional
 * shortcut and, for a row that can be chosen, an accent check.
 *
 * The row is a 48dp touch target with a 40dp highlight inside it (6dp
 * corners, 4dp from the panel edge) that shows on press, hover and keyboard
 * focus, one surface step darker; a destructive row is in the strong danger
 * colour and highlights with the danger tint. A disabled row is in the
 * disabled text colour and cannot be activated.
 *
 * TalkBack reads the text and the shortcut as one item that can be
 * activated. A row with [selected] set also says whether it is selected, so
 * a "Sort by" group reads like a list of options.
 *
 * @param text the label of the action or option.
 * @param onClick called when the row is activated. Close the menu here.
 * @param modifier the modifier of the row.
 * @param enabled whether the row can be activated.
 * @param destructive whether the action deletes or removes something; the row is shown in the danger colour.
 * Confirm the action in a dialog when it cannot be undone.
 * @param selected `true` or `false` for an option that can be chosen, shown with an accent check when `true`;
 * `null` (the default) for an action.
 * @param shortcut a keyboard shortcut shown at the end, for example "Ctrl+D".
 * @param leadingIcon an outlined 20dp icon at the start, for example a `MonarchIcon` with a `null` content
 * description. It takes the muted text colour.
 * @param interactionSource the interaction source of the row; Monarch makes one when `null`.
 */
@Composable
@Suppress("LongParameterList")
public fun MonarchMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    destructive: Boolean = false,
    selected: Boolean? = null,
    shortcut: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    val tokens = MonarchMenuTokens
    MonarchMenuItem(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        destructive = destructive,
        selected = selected,
        leadingIcon = leadingIcon,
        trailingContent = shortcut?.let { keys ->
            {
                Text(
                    text = keys,
                    style = tokens.shortcutStyle(MonarchTheme.typography),
                    color = if (enabled) MonarchTheme.colors.textSubtle else LocalContentColor.current,
                )
            }
        },
        interactionSource = interactionSource,
    ) {
        Text(text = text, color = LocalContentColor.current)
    }
}

/**
 * A row of a [MonarchMenu] with its own content, for a row that needs more than one line of text: a name with a
 * handle under it, or a label with a count. It has the look, states and TalkBack behaviour of the text row above.
 *
 * Text in [content] takes the row's text style and colour, so a plain `Text(name)` is right; a second line can
 * use the caption style and the muted colour.
 *
 * @param onClick called when the row is activated. Close the menu here.
 * @param modifier the modifier of the row.
 * @param enabled whether the row can be activated.
 * @param destructive whether the action deletes or removes something; the row is shown in the danger colour.
 * @param selected `true` or `false` for an option that can be chosen, shown with an accent check when `true`;
 * `null` (the default) for an action.
 * @param leadingIcon an outlined 20dp icon or a small avatar at the start. It takes the muted text colour.
 * @param trailingContent content at the end before the check, for example a count or a shortcut.
 * @param interactionSource the interaction source of the row; Monarch makes one when `null`.
 * @param selectionMark what marks the row when [selected] is `true`, in place of the accent check, for example a dot.
 * @param content the text of the row, in a column that takes the free width.
 */
@Composable
@Suppress("LongParameterList")
public fun MonarchMenuItem(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    destructive: Boolean = false,
    selected: Boolean? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
    selectionMark: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = MonarchMenuTokens
    val interactions = interactionSource ?: remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val hovered by interactions.collectIsHoveredAsState()
    val focused by interactions.collectIsFocusedAsState()
    val colors = tokens.itemColors(MonarchTheme.colors, enabled, destructive, pressed || hovered || focused)
    val activation = if (selected != null) {
        Modifier.selectable(
            selected = selected,
            enabled = enabled,
            interactionSource = interactions,
            indication = null,
            onClick = onClick,
        )
    } else {
        Modifier.clickable(enabled = enabled, interactionSource = interactions, indication = null, onClick = onClick)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = tokens.RowTarget)
            .then(activation)
            .padding(tokens.Inset)
            .background(colors.highlight, MonarchTheme.shapes.sm)
            .padding(horizontal = tokens.RowPadding, vertical = tokens.Inset),
        horizontalArrangement = Arrangement.spacedBy(tokens.RowGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            CompositionLocalProvider(LocalContentColor provides colors.icon, content = leadingIcon)
        }
        CompositionLocalProvider(
            LocalContentColor provides colors.text,
            LocalTextStyle provides tokens.textStyle(MonarchTheme.typography),
        ) {
            Column(modifier = Modifier.weight(1f), content = content)
            trailingContent?.invoke()
        }
        if (selected != null) {
            Box(Modifier.size(tokens.CheckSize)) {
                if (selected && selectionMark != null) {
                    selectionMark()
                } else if (selected) {
                    val check = if (enabled) MonarchTheme.colors.accentStrong else colors.text
                    MonarchStrokeGlyphMark(MonarchStrokeGlyph.Check, check, Modifier.size(tokens.CheckSize))
                }
            }
        }
    }
}
