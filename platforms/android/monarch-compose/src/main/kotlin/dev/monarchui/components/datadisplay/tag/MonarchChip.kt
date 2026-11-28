package dev.monarchui.components.datadisplay.tag

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.monarchui.R
import dev.monarchui.accessibility.monarchFocusRing
import dev.monarchui.accessibility.monarchMinimumTouchTarget
import dev.monarchui.foundation.MonarchControlMetrics
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.internal.MonarchStrokeGlyph
import dev.monarchui.internal.MonarchStrokeGlyphMark
import dev.monarchui.internal.MonarchSurfaceColors
import dev.monarchui.internal.monarchSurface
import dev.monarchui.theme.MonarchTheme

/**
 * A chip the user can switch on and off, for example a filter: the Monarch tag with a surface fill and a default
 * border when off, the accent subtle fill with accent text and the accent border when on. There is no check
 * icon: the state is the fill and border, and TalkBack says "selected".
 *
 * It replaces the Material 3 `FilterChip` (8dp corners, a check icon, 32dp height) with the Monarch tag shape:
 * 28dp high (24dp at [MonarchTagSize.Sm]), 8dp corners, a 48dp touch target around it, the focus ring for
 * keyboard and D-pad focus, and a label that wraps at large font sizes.
 *
 * Migrating from Material: `FilterChip(selected, onClick = { set(!selected) }, label = { Text(t) }, leadingIcon = i)`
 * becomes `MonarchChip(text = t, selected = selected, onSelectedChange = set, leadingIcon = i)`.
 *
 * @param text the label.
 * @param selected whether the chip is on.
 * @param onSelectedChange called with the new state when the user toggles the chip. Not called when not [enabled].
 * @param modifier the modifier of the chip.
 * @param size the text style and visible height.
 * @param enabled whether the user can toggle the chip.
 * @param leadingIcon a decorative icon before the text, for example a `MonarchIcon` of size `Sm`.
 * @param selectedIcon shown in place of [leadingIcon] while [selected]; `null` shows none, as on the web.
 * @param secondaryText a second line under [text] in the caption style, such as seats left or a price. The chip
 * grows to two lines; TalkBack reads both.
 * @param interactionSource the source of the press, focus and hover interactions of the chip.
 * @param neutral draws the chip as a muted grey fill while [selected], not in the accent colour, for a toolbar of
 * filters and toggles, as the web log viewer does.
 */
@Composable
public fun MonarchChip(
    text: String,
    selected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    size: MonarchTagSize = MonarchTagSize.Md,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    selectedIcon: (@Composable () -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
    secondaryText: String? = null,
    neutral: Boolean = false,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val theme = MonarchTheme.colors
    val base = MonarchTagTokens.selectableColors(selected, enabled, theme, neutral)
    // Pressed is one surface step: the muted fill when off, the subtle hover fill when on.
    val colors = when {
        !pressed || !enabled -> base
        selected -> base.copy(container = theme.accentSubtleHover)
        else -> base.copy(container = theme.bgMuted)
    }
    ChipBody(
        text = text,
        colors = colors,
        size = size,
        leading = if (selected && selectedIcon != null) selectedIcon else leadingIcon,
        trailing = null,
        secondaryText = secondaryText,
        modifier = modifier
            .monarchMinimumTouchTarget()
            .monarchFocusRing(MonarchShapeDefaults.Control)
            .selectable(
                selected = selected,
                interactionSource = source,
                indication = null,
                enabled = enabled,
                role = Role.Checkbox,
                onClick = { onSelectedChange(!selected) },
            ),
    )
}

/**
 * A chip the user can switch on and off whose content is two lines (or any column) that you draw: a time over the
 * seats left, a plan over its price. The shape, fill, border, focus ring and 48dp touch target are those of the
 * text chip; the colour of the lines follows the state, so `Text` in [content] needs no colour. TalkBack reads the
 * texts as one element with the selected state.
 *
 * Give a text chip ([MonarchChip] with `text`) the second line with `secondaryText` when it is only a caption;
 * use this overload when a line needs its own style, such as a struck-through time.
 *
 * @param selected whether the chip is on.
 * @param onSelectedChange called with the new state when the user toggles the chip. Not called when not [enabled].
 * @param modifier the modifier of the chip; set a minimum width or height here.
 * @param enabled whether the user can toggle the chip.
 * @param singleChoice the chip is one option of a group of which one can be chosen: TalkBack announces a radio
 * button, not a checkbox. Put the chips in a `selectableGroup`.
 * @param interactionSource the source of the press, focus and hover interactions of the chip.
 * @param content the lines, centred in a column. Text uses the chip's colour.
 */
@Composable
public fun MonarchChip(
    selected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    singleChoice: Boolean = false,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val theme = MonarchTheme.colors
    val base = MonarchTagTokens.selectableColors(selected, enabled, theme)
    val colors = when {
        !pressed || !enabled -> base
        selected -> base.copy(container = theme.accentSubtleHover)
        else -> base.copy(container = theme.bgMuted)
    }
    ChipFrame(
        colors = colors,
        size = MonarchTagSize.Md,
        modifier = modifier
            .monarchMinimumTouchTarget()
            .monarchFocusRing(MonarchShapeDefaults.Control)
            .selectable(
                selected = selected,
                interactionSource = source,
                indication = null,
                enabled = enabled,
                role = if (singleChoice) Role.RadioButton else Role.Checkbox,
                onClick = { onSelectedChange(!selected) },
            ),
    ) { framed, _ ->
        Column(
            modifier = framed,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            content = content,
        )
    }
}

/**
 * A chip the user can remove, for example a recipient or a label on a record: the Monarch tag with a close mark
 * after the text. It replaces the Material 3 `InputChip` and `AssistChip` used as removable chips.
 *
 * The whole chip is the remove action, as in Material 3 input chips: TalkBack reads it as a button named
 * "Remove" and the text, and Delete or Backspace on a hardware keyboard removes a focused chip.
 *
 * @param text the label.
 * @param onRemove called when the user activates the chip. Not called when not [enabled].
 * @param modifier the modifier of the chip.
 * @param tone the colour role.
 * @param size the text style and visible height.
 * @param enabled whether the user can remove the chip.
 * @param leadingIcon a decorative icon before the text.
 * @param removeIcon the close mark; `null` draws the Monarch close mark. It is decorative.
 * @param removeLabel the TalkBack name. `null` uses the built-in string `monarch_tag_remove`, "Remove" and the text.
 * @param interactionSource the source of the press, focus and hover interactions of the chip.
 */
@Composable
public fun MonarchChip(
    text: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    tone: MonarchTone = MonarchTone.Neutral,
    size: MonarchTagSize = MonarchTagSize.Md,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    removeIcon: (@Composable () -> Unit)? = null,
    removeLabel: String? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    val theme = MonarchTheme.colors
    val colors = if (enabled) MonarchTagTokens.colors(tone, theme) else MonarchTagTokens.disabledColors(theme)
    val name = removeLabel ?: stringResource(R.string.monarch_tag_remove, text)
    ChipBody(
        text = text,
        colors = colors,
        size = size,
        leading = leadingIcon,
        trailing = removeIcon ?: { CloseMark(colors.content) },
        modifier = modifier
            .monarchMinimumTouchTarget()
            .monarchFocusRing(MonarchShapeDefaults.Control)
            .semantics { contentDescription = name }
            .onPreviewKeyEvent { event ->
                val delete = event.key == Key.Delete || event.key == Key.Backspace
                if (enabled && delete && event.type == KeyEventType.KeyUp) {
                    onRemove()
                    true
                } else {
                    false
                }
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onRemove,
            ),
    )
}

/** The Monarch close mark, 14dp, in [color]. Decorative. */
@Composable
private fun CloseMark(color: Color) {
    MonarchStrokeGlyphMark(MonarchStrokeGlyph.Cross, color, Modifier.size(MonarchControlMetrics.CloseMark))
}

/** The pill shared by the chips: [modifier] carries the touch target, focus ring and interaction. */
@Composable
private fun ChipBody(
    text: String,
    colors: MonarchSurfaceColors,
    size: MonarchTagSize,
    leading: (@Composable () -> Unit)?,
    trailing: (@Composable () -> Unit)?,
    modifier: Modifier,
    secondaryText: String? = null,
) {
    val textStyle = MonarchTagTokens.textStyle(size, MonarchTheme.typography)
    ChipFrame(colors = colors, size = size, modifier = modifier) { framed, content ->
        Row(
            modifier = framed,
            horizontalArrangement = Arrangement.spacedBy(MonarchTagTokens.ContentGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) Box(Modifier.clearAndSetSemantics { }) { leading() }
            if (secondaryText == null) {
                Text(text = text, style = textStyle, color = content)
            } else {
                Column {
                    Text(text = text, style = textStyle, color = content)
                    Text(text = secondaryText, style = MonarchTheme.typography.caption, color = content)
                }
            }
            if (trailing != null) Box(Modifier.clearAndSetSemantics { }) { trailing() }
        }
    }
}

/**
 * The pill: animated fill, text and border, the surface shape and the padding. [content] receives the modifier
 * that draws them and the current text colour, and lays out what is inside.
 */
@Composable
private fun ChipFrame(
    colors: MonarchSurfaceColors,
    size: MonarchTagSize,
    modifier: Modifier,
    content: @Composable (framed: Modifier, text: Color) -> Unit,
) {
    val motion = MonarchTheme.motion
    val container by animateColorAsState(colors.container, motion.spec(motion.fast), label = "chip fill")
    val text by animateColorAsState(colors.content, motion.spec(motion.fast), label = "chip text")
    val border by animateColorAsState(
        colors.border ?: Color.Transparent,
        motion.spec(motion.fast),
        label = "chip border",
    )
    val framed = modifier
        .monarchSurface(MonarchSurfaceColors(container, text, border), MonarchShapeDefaults.Control)
        .heightIn(min = MonarchTagTokens.minHeight(size))
        .padding(horizontal = MonarchTagTokens.HorizontalPadding, vertical = MonarchTagTokens.VerticalPadding)
    CompositionLocalProvider(LocalContentColor provides text) { content(framed, text) }
}
