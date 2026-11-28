package dev.monarchui.components.actions.buttongroup

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import dev.monarchui.accessibility.MonarchTouchTarget

/**
 * Reserves at least 48dp by 48dp around a button of a row, like
 * `monarchMinimumTouchTarget`, but reports the intrinsic size of the visible
 * button. The row asks every button for its intrinsic height to give all of
 * them the height of the tallest; the shared modifier would report 48dp.
 */
internal fun Modifier.monarchButtonGroupTouchTarget(): Modifier = this then ButtonGroupTouchTargetElement

private data object ButtonGroupTouchTargetElement : ModifierNodeElement<ButtonGroupTouchTargetNode>() {
    override fun create(): ButtonGroupTouchTargetNode = ButtonGroupTouchTargetNode()

    override fun update(node: ButtonGroupTouchTargetNode) = Unit

    override fun InspectorInfo.inspectableProperties() {
        name = "monarchButtonGroupTouchTarget"
    }
}

private class ButtonGroupTouchTargetNode : Modifier.Node(), LayoutModifierNode {
    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        val minimum = MonarchTouchTarget.Minimum.roundToPx()
        val width = maxOf(placeable.width, minimum)
        val height = maxOf(placeable.height, minimum)
        return layout(width, height) {
            placeable.place((width - placeable.width) / 2, (height - placeable.height) / 2)
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(measurable: IntrinsicMeasurable, width: Int): Int =
        measurable.minIntrinsicHeight(width)

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(measurable: IntrinsicMeasurable, width: Int): Int =
        measurable.maxIntrinsicHeight(width)

    override fun IntrinsicMeasureScope.minIntrinsicWidth(measurable: IntrinsicMeasurable, height: Int): Int =
        measurable.minIntrinsicWidth(height)

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(measurable: IntrinsicMeasurable, height: Int): Int =
        measurable.maxIntrinsicWidth(height)
}
