package com.rookhq.monarch.components.overlays.menu

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.theme.MonarchTheme
import com.rookhq.monarch.tokens.MonarchTokens

/**
 * A menu of actions or options that opens from a control: More on a row,
 * Sort, the actions of a document. Fill it with [MonarchMenuItem],
 * [MonarchMenuLabel] and [MonarchMenuDivider].
 *
 * It is the Material 3 `DropdownMenu`, so the platform keeps the popup, its
 * position next to the anchor (flipped when there is no room, mirrored in
 * right to left), focus, back and Escape, the keyboard and the open and
 * close motion (none when animations are off). Monarch draws the panel: 192
 * to 352dp wide, 12dp corners, the raised background with a subtle hairline,
 * a soft shadow in the light themes, and 4dp around the rows.
 *
 * Place it in a `Box` with its anchor, as the Material menu:
 *
 * ```
 * Box {
 *     MonarchIconButton(label = "More actions", onClick = { open = true }) { … }
 *     MonarchMenu(expanded = open, onDismissRequest = { open = false }) {
 *         MonarchMenuItem(text = "Rename", onClick = { … }, leadingIcon = { … })
 *         MonarchMenuDivider()
 *         MonarchMenuItem(text = "Delete", onClick = { … }, destructive = true)
 *     }
 * }
 * ```
 *
 * @param expanded whether the menu is open.
 * @param onDismissRequest called when the person taps outside, goes back or presses Escape. Set [expanded] to
 * `false` in it; an item closes the menu itself in its `onClick`.
 * @param modifier the modifier of the column of rows.
 * @param offset moves the menu from its default place next to the anchor.
 * @param content the rows.
 */
@Composable
public fun MonarchMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(MonarchTokens.Space.space0.dp, MonarchTokens.Space.space1.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MonarchTheme.colors
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = Modifier
            .monarchTrimVertical(MonarchMenuTokens.MaterialPadding)
            .sizeIn(minWidth = MonarchMenuTokens.MinWidth, maxWidth = MonarchMenuTokens.MaxWidth)
            .then(modifier),
        offset = offset,
        shape = MonarchTheme.shapes.lg,
        containerColor = colors.bgRaised,
        tonalElevation = MonarchTokens.Space.space0.dp,
        shadowElevation = if (MonarchTheme.isDark) MonarchTokens.Space.space0.dp else MonarchMenuTokens.ShadowElevation,
        border = BorderStroke(
            MonarchShapeDefaults.BorderWidth,
            MonarchMenuTokens.border(colors, MonarchTheme.isDark, MonarchTheme.isHighContrast),
        ),
        content = content,
    )
}
