package com.rookhq.monarch.accessibility

import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusEventModifierNode
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.theme.LocalMonarchColors
import com.rookhq.monarch.theme.LocalMonarchThemeTokens

/**
 * Draws the Monarch focus ring around this element while it, or an element
 * inside it, has focus.
 *
 * The ring uses the `focusRing` colour and the focus ring width of the active
 * theme, and is drawn outside the element with a gap of the same width. Put
 * the modifier before the one that makes the element focusable (`clickable`,
 * `toggleable`, `focusable`).
 *
 * Compose gives focus to buttons and similar elements only for keyboard,
 * D-pad and switch navigation, so the ring does not appear on touch.
 *
 * @param shape the outline of the element.
 */
public fun Modifier.monarchFocusRing(shape: Shape): Modifier = this then FocusRingElement(shape)

private data class FocusRingElement(private val shape: Shape) : ModifierNodeElement<FocusRingNode>() {
    override fun create(): FocusRingNode = FocusRingNode(shape)

    override fun update(node: FocusRingNode) {
        node.updateShape(shape)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "monarchFocusRing"
        properties["shape"] = shape
    }
}

private class FocusRingNode(private var shape: Shape) :
    Modifier.Node(),
    FocusEventModifierNode,
    DrawModifierNode,
    CompositionLocalConsumerModifierNode {
    private var focused = false

    fun updateShape(next: Shape) {
        if (next != shape) {
            shape = next
            invalidateDraw()
        }
    }

    override fun onFocusEvent(focusState: FocusState) {
        if (focusState.hasFocus != focused) {
            focused = focusState.hasFocus
            invalidateDraw()
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        if (!focused) return
        // Read while drawing: Compose redraws this node when the theme it is under changes.
        val color = currentValueOf(LocalMonarchColors).focusRing
        val width = currentValueOf(LocalMonarchThemeTokens).focusRingWidth
        val stroke = width.dp.toPx()
        // The centre of the stroke sits one and a half widths outside the element: a gap, then the ring.
        val outset = stroke * GAP_AND_HALF_STROKE
        val ringSize = Size(size.width + outset * 2, size.height + outset * 2)
        val outline = shape.createOutline(ringSize, layoutDirection, this)
        translate(left = -outset, top = -outset) {
            drawOutline(outline = outline, color = color, style = Stroke(width = stroke))
        }
    }

    private companion object {
        const val GAP_AND_HALF_STROKE = 1.5f
    }
}
