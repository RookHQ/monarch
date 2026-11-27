package com.rookhq.monarch.components.workflow.gantttimeline

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import com.rookhq.monarch.internal.MonarchDefaultIcons
import androidx.compose.ui.graphics.lerp
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.rookhq.monarch.accessibility.monarchFocusRing
import com.rookhq.monarch.foundation.monarchShadow
import com.rookhq.monarch.theme.MonarchTheme

/** Where the bar's node sits in the grid, kept up to date so a drag is measured against the grid, not the bar. */
private class GanttTimelineOrigin {
    var x: Float = 0f

    /** Set when a touch and hold picked the bar up, so the release does not also open it. */
    var held: Boolean = false
}

/**
 * One item's bar or one-day marker: a node at least 48dp wide and as tall as the row, with the visible bar centred
 * in it. It opens the item when tapped (with `onOpen`), and when editable it moves by touch and hold then drag,
 * by the arrow keys and by TalkBack actions.
 */
@Suppress("LongParameterList", "LongMethod", "CyclomaticComplexMethod")
@Composable
internal fun GanttTimelineBar(
    model: GanttTimelineModel,
    item: MonarchGanttTimelineItem,
    span: GanttTimelineSpan,
    point: Boolean,
    visibleWidth: Dp,
    barHeight: Dp,
    scale: GanttTimelineScale,
    dragging: Boolean,
    onDrag: (GanttTimelineDrag?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MonarchGanttTimelineTokens
    val colors = tokens.barColors(MonarchTheme.colors, item.tone, item.conflict != null)
    val shape = MonarchTheme.shapes.md
    val editable = model.editable(item)
    val open = model.onOpen
    val description = model.describe(item, span)
    val actions = if (editable) model.actions(item, point) else emptyList()
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val origin = remember { GanttTimelineOrigin() }
    val current by rememberUpdatedState(GanttTimelineBarInput(model, item, scale.dayWidth, point, onDrag))
    val visiblePx = with(density) { visibleWidth.toPx() }
    val edgePx = with(density) { tokens.EdgeZone.toPx() }
    val interaction = when {
        open != null -> Modifier
            .monarchFocusRing(shape)
            .clickable(role = Role.Button) {
                if (!origin.held) open(item)
            }
        editable -> Modifier.monarchFocusRing(shape).focusable()
        else -> Modifier
    }
    val gestures = if (editable) {
        Modifier
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                val days = when (event.key) {
                    Key.DirectionLeft -> -1
                    Key.DirectionRight -> 1
                    else -> return@onKeyEvent false
                }
                val shift = when {
                    event.isShiftPressed -> MonarchGanttTimelineShift.End
                    event.isAltPressed -> MonarchGanttTimelineShift.Start
                    else -> MonarchGanttTimelineShift.Move
                }
                current.model.shift(current.item, shift, days)
                true
            }
            .pointerInput(Unit) {
                // A new touch forgets the previous hold, so only the release of a hold skips opening.
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    origin.held = false
                }
            }
            .pointerInput(item.id, visiblePx, edgePx) {
                var shift = MonarchGanttTimelineShift.Move
                var startX = 0f
                var days = 0
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        val inset = (size.width - visiblePx) / 2
                        shift = GanttTimelineRules.dragShift(offset.x - inset, visiblePx, edgePx, current.point)
                        startX = offset.x + origin.x
                        days = 0
                        origin.held = true
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        current.onDrag(GanttTimelineDrag(current.item.id, shift, 0))
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val next = GanttTimelineRules.dragDays(change.position.x + origin.x - startX, current.dayWidth)
                        if (next != days) {
                            days = next
                            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            current.onDrag(GanttTimelineDrag(current.item.id, shift, days))
                        }
                    },
                    onDragEnd = {
                        current.onDrag(null)
                        if (days != 0) current.model.shift(current.item, shift, days)
                    },
                    onDragCancel = { current.onDrag(null) },
                )
            }
    } else {
        Modifier
    }
    Box(
        modifier = modifier
            .onPlaced { origin.x = it.positionInParent().x }
            .semantics(mergeDescendants = true) {
                contentDescription = description
                if (actions.isNotEmpty()) customActions = actions
            }
            .then(interaction)
            .then(gestures),
        contentAlignment = Alignment.Center,
    ) {
        if (point) {
            Box(
                Modifier
                    .size(tokens.PointSize)
                    .rotate(tokens.DIAMOND_DEGREES)
                    .background(colors.border, MonarchTheme.shapes.xs),
            )
        } else {
            GanttTimelineBarFace(item, colors, shape, visibleWidth, barHeight, dragging)
        }
    }
}

/** What the bar's gesture reads at the moment it runs. */
private class GanttTimelineBarInput(
    val model: GanttTimelineModel,
    val item: MonarchGanttTimelineItem,
    val dayWidth: Float,
    val point: Boolean,
    val onDrag: (GanttTimelineDrag?) -> Unit,
)

/** The visible bar: the tint, the progress strip along the bottom, the outline (dashed for a conflict), the label. */
@Composable
private fun GanttTimelineBarFace(
    item: MonarchGanttTimelineItem,
    colors: GanttTimelineBarColors,
    shape: Shape,
    width: Dp,
    height: Dp,
    dragging: Boolean,
) {
    val tokens = MonarchGanttTimelineTokens
    val border = if (colors.dashed) tokens.ConflictBorderWidth else tokens.BorderWidth
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .then(if (dragging) Modifier.monarchShadow(MonarchTheme.elevation.lg, shape) else Modifier)
            .clip(shape)
            .background(colors.container)
            .drawWithContent {
                drawContent()
                val stroke = border.toPx()
                val dash = tokens.DashLength.toPx()
                drawOutline(
                    outline = shape.createOutline(size, layoutDirection, this),
                    color = colors.border,
                    style = Stroke(
                        width = stroke * 2,
                        pathEffect = if (colors.dashed) PathEffect.dashPathEffect(floatArrayOf(dash, dash)) else null,
                    ),
                )
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        // The web fills the finished part of the bar with a stronger tint, rather than a strip along the bottom.
        item.progress?.let { progress ->
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(lerp(colors.container, colors.progress, tokens.ProgressFillStrength)),
            )
        }
        if (width >= tokens.EdgeZone * tokens.LABEL_MIN_EDGES) {
            Row(
                modifier = Modifier.padding(horizontal = tokens.BarPadding),
                horizontalArrangement = Arrangement.spacedBy(tokens.BarIconGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // A conflict is also marked by a warning glyph, as on the web, so it is not the dashes alone.
                if (item.conflict != null) {
                    CompositionLocalProvider(LocalContentColor provides colors.progress) {
                        MonarchDefaultIcons.Warning()
                    }
                }
                Text(
                    text = item.label,
                    modifier = Modifier.weight(1f, fill = false).clearAndSetSemantics {},
                    style = MonarchTheme.typography.caption,
                    color = colors.content,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
