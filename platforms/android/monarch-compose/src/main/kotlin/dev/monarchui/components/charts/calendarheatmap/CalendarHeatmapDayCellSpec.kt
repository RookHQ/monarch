package dev.monarchui.components.charts.calendarheatmap

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

/**
 * What a day cell needs besides its place: its look, what TalkBack reads, and what a tap and the Select action
 * do. A day takes a tap through a pointer handler rather than a click, because year days are far below the 48dp
 * target; TalkBack users select with the custom action instead.
 */
internal class CalendarHeatmapDayCellSpec(
    val style: CalendarHeatmapDayStyle,
    val spoken: String,
    val selected: Boolean,
    val radius: Dp,
    val selectLabel: String,
    val onTap: () -> Unit,
    val onSelect: (() -> Unit)?,
)

/** One day: its fill and outlines drawn behind [content], one TalkBack element, a tap handler. */
@Composable
internal fun CalendarHeatmapDayCell(
    spec: CalendarHeatmapDayCellSpec,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {},
) {
    val tap by rememberUpdatedState(spec.onTap)
    Box(
        modifier = modifier
            .drawBehind { drawCalendarHeatmapDay(spec.style, spec.radius.toPx()) }
            .pointerInput(Unit) { detectTapGestures { tap() } }
            .clearAndSetSemantics {
                contentDescription = spec.spoken
                if (spec.selected) selected = true
                val select = spec.onSelect
                if (select != null) {
                    customActions = listOf(
                        CustomAccessibilityAction(spec.selectLabel) {
                            select()
                            true
                        },
                    )
                }
            },
    ) { content() }
}

/** Draws the fill, the outline (hairline, dashed or thick) and the marker dot in the top end corner. */
internal fun DrawScope.drawCalendarHeatmapDay(style: CalendarHeatmapDayStyle, radiusPx: Float) {
    val radius = CornerRadius(minOf(radiusPx, size.minDimension * MonarchCalendarHeatmapTokens.CORNER_SHARE))
    drawRoundRect(color = style.fill, cornerRadius = radius)
    val width = when (style.outlineWidth) {
        CalendarHeatmapOutline.None -> 0f
        CalendarHeatmapOutline.Thin -> MonarchCalendarHeatmapTokens.ThinStroke.toPx()
        CalendarHeatmapOutline.Thick -> MonarchCalendarHeatmapTokens.ThickStroke.toPx()
    }
    if (width > 0f) {
        val dash = MonarchCalendarHeatmapTokens.Dash.toPx()
        drawRoundRect(
            color = style.outline,
            topLeft = Offset(width / 2, width / 2),
            size = Size(size.width - width, size.height - width),
            cornerRadius = radius,
            style = Stroke(
                width = width,
                pathEffect = if (style.dashed) PathEffect.dashPathEffect(floatArrayOf(dash, dash)) else null,
            ),
        )
    }
    val marker = style.marker ?: return
    val diameter = minOf(MonarchCalendarHeatmapTokens.Marker.toPx(), size.minDimension / 2)
    val inset = minOf(
        MonarchCalendarHeatmapTokens.MarkerInset.toPx(),
        size.minDimension * MonarchCalendarHeatmapTokens.CORNER_SHARE,
    )
    val x = if (layoutDirection == LayoutDirection.Rtl) inset + diameter / 2 else size.width - inset - diameter / 2
    drawCircle(color = marker, radius = diameter / 2, center = Offset(x, inset + diameter / 2))
}
