package com.rookhq.monarch.components.interaction.draghandle

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.MonarchSurfaceColors
import com.rookhq.monarch.internal.MonarchTonePalette
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens of MonarchDragHandle, built from primitives and roles only. */
internal object MonarchDragHandleTokens {
    /** The side of the grip glyph. */
    val GlyphSize: Dp = MonarchTokens.Space.space4.dp

    /** The outline of the grabbed handle, so the state does not rest on colour alone. */
    val GrabbedOutline: Dp = MonarchTokens.Space.px.dp

    /** The glyph is drawn on a 16-unit grid, as the web's SVG. */
    const val GRID: Float = 16f

    /** The radius of a dot on that grid. */
    const val DOT_RADIUS: Float = 1.25f

    private val near = listOf(6f, 10f)
    private val far = listOf(4f, 8f, 12f)

    /** The visible side of the handle. */
    fun visibleSize(size: MonarchDragHandleSize): Dp = when (size) {
        MonarchDragHandleSize.Sm -> MonarchTokens.Space.space6.dp
        MonarchDragHandleSize.Md -> MonarchTokens.Control.sm.dp
    }

    /** The centres of the grip dots on the 16-unit grid: two columns of three, three rows of two, or three by three. */
    fun dots(axis: MonarchDragHandleAxis): List<Offset> = when (axis) {
        MonarchDragHandleAxis.Vertical -> near.flatMap { x -> far.map { y -> Offset(x, y) } }
        MonarchDragHandleAxis.Horizontal -> far.flatMap { x -> near.map { y -> Offset(x, y) } }
        MonarchDragHandleAxis.Both -> far.flatMap { x -> far.map { y -> Offset(x, y) } }
    }

    /** The move actions offered for an axis, in the order TalkBack lists them. */
    fun directions(axis: MonarchDragHandleAxis): List<MonarchDragHandleDirection> = when (axis) {
        MonarchDragHandleAxis.Vertical -> listOf(MonarchDragHandleDirection.Up, MonarchDragHandleDirection.Down)
        MonarchDragHandleAxis.Horizontal ->
            listOf(MonarchDragHandleDirection.Backward, MonarchDragHandleDirection.Forward)
        MonarchDragHandleAxis.Both -> MonarchDragHandleDirection.entries
    }

    /**
     * The fill, dot colour and outline of the handle. The fill is transparent at rest; the outline is drawn
     * only while grabbed.
     */
    fun colors(state: MonarchDragHandleState, enabled: Boolean, colors: MonarchColors): MonarchSurfaceColors = when {
        !enabled -> MonarchSurfaceColors(Color.Transparent, colors.textDisabled, null)
        state == MonarchDragHandleState.Grabbed -> MonarchTonePalette.of(MonarchTone.Accent, colors).let {
            MonarchSurfaceColors(it.subtle, it.strong, it.border)
        }
        state == MonarchDragHandleState.Dropped -> MonarchTonePalette.of(MonarchTone.Success, colors).let {
            MonarchSurfaceColors(it.subtle, it.strong, null)
        }
        else -> MonarchSurfaceColors(Color.Transparent, colors.textMuted, null)
    }
}
