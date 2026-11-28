package com.rookhq.monarch.components.workflow.orgchart

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates

/**
 * Where the cards and chips of a `MonarchOrgChart` are, in window
 * coordinates, and the person being dragged. The chart draws its connector
 * lines from the positions and the drag hit-tests against them.
 */
@Stable
internal class MonarchOrgChartDragState {
    /** The window bounds of every card on screen, by node id. */
    val cards = mutableStateMapOf<String, Rect>()

    /** The window bounds of every team chip on screen, by node id. */
    val chips = mutableStateMapOf<String, Rect>()

    /** The window bounds of the area a person can be dropped on. */
    var area: Rect = Rect.Zero

    /** The id of the person being dragged. */
    var dragged: String? by mutableStateOf(null)
        private set

    /** How far the dragged card has moved, in its own coordinates. */
    var offset: Offset by mutableStateOf(Offset.Zero)
        private set

    /** The id of the card the dragged person would be dropped on, when that move is allowed. */
    var target: String? by mutableStateOf(null)
        private set

    private var overBackground = false

    fun start(id: String) {
        dragged = id
        offset = Offset.Zero
        target = null
        overBackground = false
    }

    /** Moves the dragged card by [amount] and finds what is under [pointer], a window position. */
    fun move(amount: Offset, pointer: Offset, tree: MonarchOrgChartTree) {
        val person = dragged ?: return
        offset += amount
        val hit = cards.entries.firstOrNull { (id, bounds) -> id != person && bounds.contains(pointer) }?.key
        target = hit?.takeIf { tree.canMove(person, it) }
        overBackground = hit == null && area.contains(pointer) && cards[person]?.contains(pointer) != true
    }

    /** Ends the drag and returns where the person was dropped, or `null` for nowhere they can go. */
    fun finish(tree: MonarchOrgChartTree): MonarchOrgChartMoveTarget? {
        val person = dragged
        val chosen = target
        val result = when {
            person == null -> null
            chosen != null -> MonarchOrgChartMoveTarget.Manager(chosen)
            overBackground && tree.canMoveToTop(person) -> MonarchOrgChartMoveTarget.TopLevel
            else -> null
        }
        cancel()
        return result
    }

    fun cancel() {
        dragged = null
        offset = Offset.Zero
        target = null
        overBackground = false
    }

    /** Records the window bounds of [coordinates] in [into] under [id], without writing an unchanged value. */
    fun place(into: MutableMap<String, Rect>, id: String, coordinates: LayoutCoordinates) {
        val bounds = monarchOrgChartWindowBounds(coordinates)
        if (into[id] != bounds) into[id] = bounds
    }
}

/** The unclipped bounds of [coordinates] in the window, after any zoom. */
internal fun monarchOrgChartWindowBounds(coordinates: LayoutCoordinates): Rect {
    val size = coordinates.size
    val topLeft = coordinates.localToWindow(Offset.Zero)
    val bottomRight = coordinates.localToWindow(Offset(size.width.toFloat(), size.height.toFloat()))
    return Rect(
        left = minOf(topLeft.x, bottomRight.x),
        top = minOf(topLeft.y, bottomRight.y),
        right = maxOf(topLeft.x, bottomRight.x),
        bottom = maxOf(topLeft.y, bottomRight.y),
    )
}
