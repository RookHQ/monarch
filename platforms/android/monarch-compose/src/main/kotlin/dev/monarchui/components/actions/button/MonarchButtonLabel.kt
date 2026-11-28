package dev.monarchui.components.actions.button

import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import dev.monarchui.internal.MonarchCentredText

/**
 * The icons and the label of the text overload of `MonarchButton`, in one row.
 *
 * The label takes the width that is left after the icons and the gaps, so a
 * long label wraps and the trailing icon stays visible. When the button has no
 * width limit, for example inside a `horizontalScroll` row, the label takes
 * its natural width. (`Modifier.weight(1f, fill = false)` gives the label no
 * width at all there, because a row has no space to share out when its width
 * is unbounded.)
 */
@Composable
internal fun MonarchButtonLabel(
    text: String,
    leadingIcon: (@Composable () -> Unit)?,
    trailingIcon: (@Composable () -> Unit)?,
) {
    Layout(
        contents = listOf(
            { leadingIcon?.invoke() },
            // Centred only when it wraps, see MonarchCentredText.
            { MonarchCentredText(text = text) },
            { trailingIcon?.invoke() },
        ),
    ) { (leading, label, trailing), constraints ->
        val gap = MonarchButtonTokens.ContentGap.roundToPx()
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val leadingPlaceables = leading.map { it.measure(loose) }
        val trailingPlaceables = trailing.map { it.measure(loose) }
        val iconGroups = listOf(leadingPlaceables, trailingPlaceables).count { it.isNotEmpty() }
        val iconsWidth = (leadingPlaceables + trailingPlaceables).sumOf { it.width } + iconGroups * gap
        val labelPlaceables = label.measureLabel(loose, iconsWidth)
        val contentWidth = iconsWidth + labelPlaceables.sumOf { it.width }
        val all = leadingPlaceables + labelPlaceables + trailingPlaceables
        val width = contentWidth.coerceIn(constraints.minWidth, constraints.maxWidth)
        val height = (all.maxOfOrNull { it.height } ?: 0).coerceIn(constraints.minHeight, constraints.maxHeight)
        layout(width, height) {
            var x = ((width - contentWidth) / 2).coerceAtLeast(0)
            fun placeRow(row: List<Placeable>, gapAfter: Boolean) {
                row.forEach { placeable ->
                    placeable.placeRelative(x, (height - placeable.height) / 2)
                    x += placeable.width
                }
                if (gapAfter && row.isNotEmpty()) x += gap
            }
            placeRow(leadingPlaceables, gapAfter = true)
            placeRow(labelPlaceables, gapAfter = trailingPlaceables.isNotEmpty())
            placeRow(trailingPlaceables, gapAfter = false)
        }
    }
}

/** Measures the label in the width left after [reserved], or at its natural width when the width is unbounded. */
private fun List<Measurable>.measureLabel(loose: Constraints, reserved: Int): List<Placeable> {
    val labelConstraints = if (loose.hasBoundedWidth) {
        loose.copy(maxWidth = (loose.maxWidth - reserved).coerceAtLeast(0))
    } else {
        loose
    }
    return map { it.measure(labelConstraints) }
}
