package dev.monarchui.components.forms.textfield

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.accessibility.MonarchTouchTarget
import dev.monarchui.internal.field.MonarchFieldIconSize
import dev.monarchui.theme.MonarchTheme
import dev.monarchui.tokens.MonarchTokens

/**
 * A real icon button inside a field (stepper, clear, copy): the [content] icon in a 48dp touch target, the
 * Button role and [name] as its spoken name. The icon is decoration.
 *
 * The 48dp target is kept for touch and TalkBack, but the button takes only the room of its visible pad
 * (the icon plus 4dp around it) in the field's row: the rest of the target overlaps the field's own padding.
 * So a trailing button's icon sits as far from the border as a leading icon, and a small field stays 40dp.
 * Pressed and hovered show one surface step on the pad; keyboard focus draws the 2dp ring around the pad.
 *
 * @param bleedStart lets the target reach past the start of its room (into the gap before it). Pass `false` for
 * the second of two adjacent buttons, so their targets never overlap.
 * @param bleedEnd lets the target reach past the end of its room (into the field's end padding). Pass `false`
 * for the first of two adjacent buttons.
 */
@Composable
internal fun MonarchFieldIconButton(
    name: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    bleedStart: Boolean = true,
    bleedEnd: Boolean = true,
    content: @Composable () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    FieldIconTarget(
        modifier = modifier,
        source = source,
        enabled = enabled,
        bleedStart = bleedStart,
        bleedEnd = bleedEnd,
        interaction = Modifier
            .semantics { contentDescription = name }
            .clickable(
                interactionSource = source,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        content = content,
    )
}

/**
 * An on/off icon inside a field (the password's eye): [MonarchFieldIconButton]'s pad, target and ring, with the
 * Checkbox role and [checked] as its state. [name] stays the same in both states; [actionLabel] says what a
 * double tap does ("Show", "Hide").
 */
@Composable
@Suppress("LongParameterList")
internal fun MonarchFieldIconToggle(
    name: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    actionLabel: String? = null,
    content: @Composable () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    FieldIconTarget(
        modifier = modifier,
        source = source,
        enabled = enabled,
        bleedStart = true,
        bleedEnd = true,
        interaction = Modifier
            .semantics {
                contentDescription = name
                if (actionLabel != null) onClick(label = actionLabel, action = null)
            }
            .toggleable(
                value = checked,
                interactionSource = source,
                indication = null,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = onCheckedChange,
            ),
        content = content,
    )
}

/** The 48dp target with [interaction] on it, around the visible pad with its pressed fill and focus ring. */
@Composable
@Suppress("LongParameterList")
private fun FieldIconTarget(
    modifier: Modifier,
    source: MutableInteractionSource,
    enabled: Boolean,
    bleedStart: Boolean,
    bleedEnd: Boolean,
    interaction: Modifier,
    content: @Composable () -> Unit,
) {
    val focused by source.collectIsFocusedAsState()
    val pressed by source.collectIsPressedAsState()
    val hovered by source.collectIsHoveredAsState()
    val colors = MonarchTheme.colors
    val shape = MonarchTheme.shapes.sm
    val pad = MonarchFieldIconButtonPad
    // The room is a layout of its own, so the button's node keeps a 48dp layout and touch area for TalkBack.
    Box(modifier.fieldButtonRoom(pad, bleedStart, bleedEnd)) {
        Box(
            modifier = Modifier.size(MonarchTouchTarget.Minimum).then(interaction),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(pad)
                    .background(if (enabled && (pressed || hovered)) colors.bgMuted else Color.Transparent, shape)
                    .then(
                        if (focused) {
                            Modifier.border(MonarchTheme.focusRingWidth, colors.focusRing, shape)
                        } else {
                            Modifier
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) { content() }
        }
    }
}

/** The visible pad of a field icon button: the 20dp icon and 4dp around it. */
internal val MonarchFieldIconButtonPad: Dp = MonarchFieldIconSize + (MonarchTokens.Space.space1 * 2).dp

/**
 * Reports only the pad's room to the row (its height, and its width plus the target's overhang on each side
 * that may not bleed) while the full target is measured and placed around it.
 */
private fun Modifier.fieldButtonRoom(pad: Dp, bleedStart: Boolean, bleedEnd: Boolean): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
        val padPx = pad.roundToPx()
        val overhangX = ((placeable.width - padPx) / 2).coerceAtLeast(0)
        val overhangY = ((placeable.height - padPx) / 2).coerceAtLeast(0)
        val start = if (bleedStart) overhangX else 0
        val end = if (bleedEnd) overhangX else 0
        layout(placeable.width - start - end, placeable.height - overhangY * 2) {
            // `placeRelative` mirrors x in right-to-left layouts, so start and end stay logical.
            placeable.placeRelative(-start, -overhangY)
        }
    }

/**
 * The − and + buttons at the end of a number or duration field: two [MonarchFieldIconButton]s whose 48dp
 * targets touch but never overlap, the glyphs in [tint] (the muted colour, or the disabled one).
 */
@Composable
@Suppress("LongParameterList")
internal fun MonarchFieldStepper(
    decreaseName: String,
    increaseName: String,
    canDecrease: Boolean,
    canIncrease: Boolean,
    tint: @Composable (enabled: Boolean) -> Color,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        MonarchFieldIconButton(name = decreaseName, onClick = onDecrease, enabled = canDecrease, bleedEnd = false) {
            MonarchFieldGlyphIcon(MonarchFieldGlyph.Minus, tint = tint(canDecrease))
        }
        MonarchFieldIconButton(name = increaseName, onClick = onIncrease, enabled = canIncrease, bleedStart = false) {
            MonarchFieldGlyphIcon(MonarchFieldGlyph.Plus, tint = tint(canIncrease))
        }
    }
}
