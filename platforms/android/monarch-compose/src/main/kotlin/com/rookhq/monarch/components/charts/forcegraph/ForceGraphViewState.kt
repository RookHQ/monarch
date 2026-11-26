package com.rookhq.monarch.components.charts.forcegraph

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

/**
 * What the user did to the view of a force graph: the zoom, the pan (in the
 * start-to-end direction) and the nodes they moved, in layout units.
 */
internal class ForceGraphViewState {
    var zoom: Double by mutableDoubleStateOf(1.0)
        private set

    var pan: Offset by mutableStateOf(Offset.Zero)
        private set

    /** The nodes moved by dragging, by id. */
    val moved: MutableMap<String, ForceGraphPoint> = mutableStateMapOf()

    /** Zooms by [factor] around the centre of the canvas, within the limits. */
    fun zoomBy(factor: Double) {
        val next = ForceGraphGeometry.zoomed(zoom, factor)
        pan *= (next / zoom).toFloat()
        zoom = next
    }

    /** Moves the view by a drag of [delta] on the screen; right to left the start is on the right. */
    fun panBy(delta: Offset, rtl: Boolean) {
        pan += Offset(if (rtl) -delta.x else delta.x, delta.y)
    }

    /** Undoes zoom, pan and moved nodes. */
    fun reset() {
        zoom = 1.0
        pan = Offset.Zero
        moved.clear()
    }

    val canZoomIn: Boolean get() = zoom < MonarchForceGraphTokens.ZOOM_MAX
    val canZoomOut: Boolean get() = zoom > MonarchForceGraphTokens.ZOOM_MIN
}
