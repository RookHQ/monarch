package dev.monarchui.accessibility

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layout
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.tokens.MonarchTokens

/** The minimum touch target of Monarch on Android. */
public object MonarchTouchTarget {
    /** 48dp, the minimum width and height of anything a user can tap. */
    public val Minimum: Dp = MonarchTokens.Space.space12.dp
}

/**
 * Reserves at least 48dp by 48dp for this element and centres it in that space.
 *
 * Put it before the modifier that handles the tap (`clickable`, `toggleable`).
 * Compose already extends the hit area of a small element to 48dp; this
 * modifier reserves the layout space, so the extended areas of two neighbours
 * cannot overlap. Monarch components apply it themselves. Use it for a custom
 * element whose visible size is smaller than 48dp.
 */
public fun Modifier.monarchMinimumTouchTarget(): Modifier = this then MinimumTouchTargetElement

/**
 * Reserves the 48dp touch target like [monarchMinimumTouchTarget], but
 * reports the intrinsic size of the visible element. Use it inside a row or
 * column that sizes its children by their intrinsic size (`IntrinsicSize.Min`,
 * a segmented row that gives every item the height of the tallest), where the
 * reported 48dp would grow the visible elements.
 */
internal fun Modifier.monarchMinimumTouchTargetWithVisibleIntrinsics(): Modifier =
    this then VisibleIntrinsicsTouchTargetElement

private data object MinimumTouchTargetElement : ModifierNodeElement<MinimumTouchTargetNode>() {
    override fun create(): MinimumTouchTargetNode = MinimumTouchTargetNode()

    override fun update(node: MinimumTouchTargetNode) = Unit

    override fun InspectorInfo.inspectableProperties() {
        name = "monarchMinimumTouchTarget"
    }
}

private data object VisibleIntrinsicsTouchTargetElement : ModifierNodeElement<VisibleIntrinsicsTouchTargetNode>() {
    override fun create(): VisibleIntrinsicsTouchTargetNode = VisibleIntrinsicsTouchTargetNode()

    override fun update(node: VisibleIntrinsicsTouchTargetNode) = Unit

    override fun InspectorInfo.inspectableProperties() {
        name = "monarchMinimumTouchTargetWithVisibleIntrinsics"
    }
}

/** Reports the intrinsic size of the visible element, not of the reserved target. */
private class VisibleIntrinsicsTouchTargetNode : MinimumTouchTargetNode() {
    override fun IntrinsicMeasureScope.minIntrinsicHeight(measurable: IntrinsicMeasurable, width: Int): Int =
        measurable.minIntrinsicHeight(width)

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(measurable: IntrinsicMeasurable, width: Int): Int =
        measurable.maxIntrinsicHeight(width)

    override fun IntrinsicMeasureScope.minIntrinsicWidth(measurable: IntrinsicMeasurable, height: Int): Int =
        measurable.minIntrinsicWidth(height)

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(measurable: IntrinsicMeasurable, height: Int): Int =
        measurable.maxIntrinsicWidth(height)
}

private open class MinimumTouchTargetNode : Modifier.Node(), LayoutModifierNode {
    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        val minimum = MonarchTouchTarget.Minimum.roundToPx()
        val width = maxOf(placeable.width, minimum)
        val height = maxOf(placeable.height, minimum)
        val top = (height - placeable.height) / 2
        return layout(width, height, mapOf(VisibleTop to top, VisibleBottom to top + placeable.height)) {
            placeable.place((width - placeable.width) / 2, top)
        }
    }
}

/**
 * The top of the visible element inside its reserved touch target. A parent sees the smallest value of its
 * children, so the line marks the top of the highest visible element.
 */
internal val VisibleTop: HorizontalAlignmentLine = HorizontalAlignmentLine(::minOf)

/** The bottom of the visible element inside its reserved touch target; a parent sees the largest value. */
internal val VisibleBottom: HorizontalAlignmentLine = HorizontalAlignmentLine(::maxOf)

/**
 * Lets the reserved touch targets of the controls inside this element overhang it at the top and the bottom.
 *
 * The element reports the height of what is visible (from the top of the highest visible control to the
 * bottom of the lowest) and the reserved space above and below spills into the padding around it. Use it on a
 * row of small buttons at the end of a card: without it, a 32dp button reserves 8dp under itself and the card
 * looks taller at the bottom than at the top. The touch targets stay 48dp, laid out and tappable, as long as
 * the padding around this element is at least the overhang (8dp for a 32dp button, 6dp for a 36dp one) and
 * the container does not clip it. A control that is taller than its target (large font) has no overhang, and
 * other content above or under the controls (more than 10dp from the edge) is never trimmed.
 */
internal fun Modifier.monarchTouchTargetOverhang(): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val most = MaxOverhang.roundToPx()
    // Only reserved space is trimmed: a gap larger than any reserve means other content (a line of text above
    // the buttons) sits there, and it stays.
    val top = placeable.lineOr(VisibleTop, 0).takeIf { it in 0..most } ?: 0
    val bottom = placeable.lineOr(VisibleBottom, placeable.height)
        .takeIf { placeable.height - it in 0..most && it > top } ?: placeable.height
    layout(placeable.width, bottom - top) {
        placeable.place(0, -top)
    }
}

/** The largest reserve: around the smallest visible control, 28dp, in a 48dp target. */
private val MaxOverhang: Dp = MonarchTokens.Space.space2_5.dp

private fun Placeable.lineOr(line: AlignmentLine, fallback: Int): Int =
    this[line].takeIf { it != AlignmentLine.Unspecified } ?: fallback
