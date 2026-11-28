package dev.monarchui.components.actions.buttongroup

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.max
import dev.monarchui.accessibility.MonarchTouchTarget
import dev.monarchui.accessibility.monarchMinimumTouchTarget
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonSurface
import dev.monarchui.components.actions.button.MonarchButtonTokens
import dev.monarchui.components.actions.button.MonarchButtonVariant

/**
 * Related buttons shown as one control, such as Previous and Next or a set
 * of zoom controls.
 *
 * Built from the Monarch button surface (the same anatomy as `MonarchButton`):
 * joined into one shape with square inner corners, or kept apart with a
 * small gap. Every button stays its own TalkBack element with the Button
 * role, its own focus stop and a touch target of at least 48dp; the group is
 * a traversal group, and its [label] is read before the buttons. In a row the
 * buttons take the height of the tallest one, so the joined shape stays
 * straight when a label wraps, and the row reserves the 48dp touch target
 * around them; in a column every button is at least 48dp high, because the
 * buttons touch.
 *
 * @param modifier the modifier of the group.
 * @param orientation a row ([Orientation.Horizontal]) or a column ([Orientation.Vertical]).
 * @param variant the visual weight of every button.
 * @param size the visible height of every button in a row.
 * @param attached joins the buttons into one shape. When `false` they keep an 8dp gap.
 * @param fullWidth fills the available width. In a row the buttons share it equally.
 * @param label what the buttons have in common, such as "Zoom", read by TalkBack. Already localised.
 * @param content declares the buttons with [MonarchButtonGroupScope.button] and
 * [MonarchButtonGroupScope.iconButton].
 */
@Composable
public fun MonarchButtonGroup(
    modifier: Modifier = Modifier,
    orientation: Orientation = Orientation.Horizontal,
    variant: MonarchButtonVariant = MonarchButtonVariant.Secondary,
    size: MonarchButtonSize = MonarchButtonSize.Md,
    attached: Boolean = true,
    fullWidth: Boolean = false,
    label: String? = null,
    content: MonarchButtonGroupScope.() -> Unit,
) {
    val entries = MonarchButtonGroupScopeImpl().apply(content).entries
    if (entries.isEmpty()) return
    val spacing = MonarchButtonGroupTokens.spacing(variant, attached)
    val group = modifier.semantics {
        isTraversalGroup = true
        if (label != null) contentDescription = label
    }
    fun shape(index: Int): Shape = MonarchButtonGroupTokens.shape(index, entries.size, orientation, attached)
    if (orientation == Orientation.Vertical) {
        Column(
            modifier = group.then(if (fullWidth) Modifier.fillMaxWidth() else Modifier.width(IntrinsicSize.Max)),
            verticalArrangement = Arrangement.spacedBy(spacing),
        ) {
            entries.forEachIndexed { index, entry ->
                GroupButton(
                    entry = entry,
                    shape = shape(index),
                    variant = variant,
                    size = size,
                    // The buttons touch, so each one is the full touch target high.
                    minHeight = MonarchTouchTarget.Minimum,
                    reserveTouchTarget = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        return
    }
    Layout(
        content = {
            entries.forEachIndexed { index, entry ->
                GroupButton(
                    entry = entry,
                    shape = shape(index),
                    variant = variant,
                    size = size,
                    minHeight = Dp.Unspecified,
                    reserveTouchTarget = false,
                    modifier = Modifier,
                )
            }
        },
        modifier = group.then(if (fullWidth) Modifier.fillMaxWidth() else Modifier),
    ) { measurables, constraints ->
        val gap = spacing.roundToPx()
        val widths = MonarchButtonGroupTokens.rowWidths(
            natural = measurables.map { it.maxIntrinsicWidth(Constraints.Infinity) },
            available = constraints.maxWidth,
            gap = gap,
            fill = fullWidth,
        )
        // Every button takes the visible height of the tallest one, so the joined shape stays straight.
        val tallest = measurables.mapIndexed { index, button -> button.minIntrinsicHeight(widths[index]) }.max()
        val placeables = measurables.mapIndexed { index, button ->
            button.measure(Constraints(minWidth = widths[index], maxWidth = widths[index], minHeight = tallest))
        }
        val width = (placeables.sumOf { it.width } + gap * (placeables.size - 1)).coerceIn(
            constraints.minWidth,
            constraints.maxWidth,
        )
        val height = placeables.maxOf { it.height }.coerceIn(constraints.minHeight, constraints.maxHeight)
        layout(width, height) {
            var x = 0
            placeables.forEach { placeable ->
                placeable.placeRelative(x, (height - placeable.height) / 2)
                x += placeable.width + gap
            }
        }
    }
}

@Composable
@Suppress("LongParameterList")
private fun GroupButton(
    entry: MonarchButtonGroupEntry,
    shape: Shape,
    variant: MonarchButtonVariant,
    size: MonarchButtonSize,
    minHeight: Dp,
    reserveTouchTarget: Boolean,
    modifier: Modifier,
) {
    val visibleHeight = if (minHeight == Dp.Unspecified) {
        MonarchButtonTokens.minHeight(size)
    } else {
        max(MonarchButtonTokens.minHeight(size), minHeight)
    }
    val description = entry.contentDescription
    MonarchButtonSurface(
        onClick = entry.onClick,
        modifier = modifier,
        variant = variant,
        size = size,
        enabled = entry.enabled,
        loading = false,
        loadingLabel = null,
        shape = shape,
        interactionSource = null,
        // Monarch reserves the touch target itself, as MonarchButton does. In a row the reserved size is
        // hidden from the intrinsic measurement, so the row can give every button the tallest visible height.
        touchTarget = if (reserveTouchTarget) {
            Modifier.monarchMinimumTouchTarget()
        } else {
            Modifier.monarchButtonGroupTouchTarget()
        },
        minHeight = visibleHeight,
        minWidth = MonarchTouchTarget.Minimum,
        semanticsExtra = { if (description != null) contentDescription = description },
    ) {
        entry.icon?.invoke()
        entry.text?.let {
            Text(text = it, modifier = Modifier.weight(1f, fill = false), textAlign = TextAlign.Center)
        }
    }
}
