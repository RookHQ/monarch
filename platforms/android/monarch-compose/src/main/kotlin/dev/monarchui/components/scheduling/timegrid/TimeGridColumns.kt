package dev.monarchui.components.scheduling.timegrid

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.accessibility.monarchFocusRing
import dev.monarchui.components.scheduling.agendalist.AgendaListDays
import dev.monarchui.components.scheduling.agendalist.MonarchAgendaEvent
import dev.monarchui.components.scheduling.eventchip.MonarchEventChip
import dev.monarchui.components.scheduling.eventchip.MonarchEventChipVariant
import dev.monarchui.theme.MonarchTheme

/** The day columns, side by side, start to end. */
@Composable
internal fun TimeGridColumns(
    columns: List<TimeGridColumn>,
    context: TimeGridContext,
    now: TimeGridWall?,
    drag: TimeGridDrag?,
    onDrag: (TimeGridDrag?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pointer = remember { TimeGridPointer() }
    Row(modifier = modifier.onPlaced { pointer.area = it }) {
        columns.forEachIndexed { index, column ->
            TimeGridDayColumn(index, column, context, now, drag, onDrag, pointer)
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun TimeGridDayColumn(
    index: Int,
    column: TimeGridColumn,
    context: TimeGridContext,
    now: TimeGridWall?,
    drag: TimeGridDrag?,
    onDrag: (TimeGridDrag?) -> Unit,
    pointer: TimeGridPointer,
) {
    val tokens = MonarchTimeGridTokens
    val range = context.range
    val hours = range.hours
    val longDay = context.formats.longDay(column.day)
    val colors = context.colors
    val slots = context.onSelectSlot
    val tapObserver = if (slots != null) {
        Modifier.pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                pointer.tapY = down.position.y
                // The slot reads the position when it is released (main pass); forget it afterwards.
                do {
                    val event = awaitPointerEvent(PointerEventPass.Final)
                } while (event.changes.any { it.pressed })
                pointer.tapY = null
            }
        }
    } else {
        Modifier
    }
    Box(
        modifier = Modifier
            .width(context.widths[index])
            .height(context.height(range.end - range.start))
            .drawBehind {
                column.shade.forEach { span ->
                    drawRect(
                        colors.shade,
                        topLeft = Offset(0f, context.y(span.top).toPx()),
                        size = Size(size.width, context.height(span.bottom - span.top).toPx()),
                    )
                }
                drawTimeGridLines(context.hourHeight.toPx(), hours.size, colors.line, tokens.LineWidth.toPx())
            }
            .semantics {
                contentDescription = longDay
                isTraversalGroup = true
            }
            .then(tapObserver),
    ) {
        if (slots != null) {
            hours.forEach { hour ->
                TimeGridSlotButton(column.day, hour, longDay, context, pointer, slots)
            }
        }
        column.blocks.forEach { block ->
            val times = context.text.range(
                context.formats.time(block.block.start),
                context.formats.time(block.block.end),
            )
            TimeGridUnavailableBlock(
                description = tokens.unavailableDescription(block.block.label, times),
                label = block.block.label,
                top = context.y(block.top),
                height = context.height(block.bottom - block.top),
                colors = colors,
            )
        }
        column.timed.forEach { placed ->
            TimeGridEvent(index, column.day, placed, context, pointer, onDrag)
        }
        if (drag != null && drag.dayIndex == index) TimeGridGhost(drag, context)
        if (now != null && now.day == column.day && now.minutes in range.start until range.end) {
            TimeGridNowLine(context.y(now.minutes), colors.now)
        }
    }
}

/** One empty hour as a button that creates an event: at the tapped time, or at the hour from TalkBack or keys. */
@Composable
private fun TimeGridSlotButton(
    day: Long,
    hour: Int,
    longDay: String,
    context: TimeGridContext,
    pointer: TimeGridPointer,
    onSelect: (MonarchTimeGridSlot) -> Unit,
) {
    val minutes = hour * TimeGridModel.MINUTES_PER_HOUR
    val name = context.text.createEvent("$longDay, ${context.formats.minutes(minutes)}")
    val hourPx = with(LocalDensity.current) { context.hourHeight.toPx() }
    Box(
        modifier = Modifier
            .padding(top = context.y(minutes))
            .fillMaxWidth()
            .height(context.hourHeight)
            .monarchFocusRing(RectangleShape)
            .clickable(role = Role.Button) {
                val chosen = pointer.tapY?.let { y ->
                    TimeGridLayout.snapDown(y / hourPx * TimeGridModel.MINUTES_PER_HOUR, context.range, context.snap)
                } ?: minutes
                pointer.tapY = null
                onSelect(MonarchTimeGridSlot(day, chosen, TimeGridModel.instant(day, chosen, context.zone)))
            }
            .semantics { contentDescription = name },
    )
}

/** One timed event: a block chip, a button when it opens, with a long-press drag and TalkBack moves when movable. */
@Suppress("LongMethod")
@Composable
private fun TimeGridEvent(
    dayIndex: Int,
    day: Long,
    placed: TimeGridPlaced,
    context: TimeGridContext,
    pointer: TimeGridPointer,
    onDrag: (TimeGridDrag?) -> Unit,
) {
    val tokens = MonarchTimeGridTokens
    val event = placed.event
    val zone = context.zone
    val text = context.text
    val formats = context.formats
    val width = context.widths[dayIndex]
    val colWidth = (width - tokens.ColumnEndInset) / placed.cols
    val actual = TimeGridModel.span(event.start, event.end, day, zone, 1)
    val minutes = actual?.let { it.bottom - it.top } ?: 0
    val time = AgendaListDays.time(event, day, zone, text.allDay, text.range, text.until, formats::time)
    val compact = minutes < tokens.COMPACT_BELOW_MINUTES
    val startsToday = TimeGridModel.wall(event.start, zone).day == day
    val visible = if (compact && startsToday) formats.time(event.start) else time.visible
    val movable = context.movable(event)
    val actions = if (movable) timeGridMoveActions(event, context) else emptyList()
    val select = context.onSelectEvent
    val haptics = LocalHapticFeedback.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val latest by rememberUpdatedState(TimeGridEventInput(event, placed, context, rtl, onDrag))
    val holder = remember { arrayOfNulls<LayoutCoordinates>(1) }

    val gestures = if (movable) Modifier.timeGridDrag(event.id, pointer, holder, haptics) { latest } else Modifier

    Box(
        modifier = Modifier
            .padding(start = colWidth * placed.col, top = context.y(placed.top))
            .size(colWidth - tokens.EventGap, context.height(placed.bottom - placed.top))
            .onPlaced { holder[0] = it }
            .then(gestures),
    ) {
        MonarchEventChip(
            title = event.title,
            modifier = Modifier
                .fillMaxSize()
                .semantics { if (actions.isNotEmpty()) customActions = actions },
            variant = MonarchEventChipVariant.Block,
            status = event.status,
            color = event.color,
            compact = compact,
            time = visible,
            timeLabel = time.spoken,
            detail = event.detail?.takeIf { minutes >= tokens.DETAIL_FROM_MINUTES },
            onClick = select?.let { open -> { if (!pointer.held) open(event) } },
        )
    }
}

/** The dashed outline that shows where a moved event will go, with its new time and title. Hidden from TalkBack. */
@Composable
private fun TimeGridGhost(drag: TimeGridDrag, context: TimeGridContext) {
    val tokens = MonarchTimeGridTokens
    val border = tokens.ghostBorder(MonarchTheme.colors, drag.event.color)
    val length = maxOf(context.height(drag.duration), tokens.EventMinHeight)
    Box(
        modifier = Modifier
            .padding(top = context.y(drag.start), end = tokens.ColumnEndInset)
            .fillMaxWidth()
            .height(length)
            .clipToBounds()
            .background(context.colors.ghostFill)
            .drawBehind {
                val stroke = tokens.GhostBorderWidth.toPx()
                val dash = tokens.GhostDash.toPx()
                drawRect(
                    border,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash))),
                )
            }
            .clearAndSetSemantics {},
    ) {
        Text(
            text = "${context.formats.minutes(drag.start)} · ${drag.event.title}",
            modifier = Modifier.padding(tokens.BlockPadding),
            style = MonarchTheme.typography.caption,
            color = context.colors.ghostText,
        )
    }
}

/**
 * The TalkBack moves of [event]: earlier and later by the snap interval
 * inside the visible hours, and to the previous and next column, each only
 * when it is possible.
 */
internal fun timeGridMoveActions(event: MonarchAgendaEvent, context: TimeGridContext): List<CustomAccessibilityAction> {
    val onMove = context.onMoveEvent ?: return emptyList()
    val zone = context.zone
    val snap = context.snap
    val range = context.range
    val wall = TimeGridModel.wall(event.start, zone)
    val duration = TimeGridLayout.durationMinutes(event, snap)
    val index = context.days.indexOf(wall.day)
    val actions = mutableListOf<CustomAccessibilityAction>()
    fun add(label: String, day: Long, minutes: Int) {
        actions += CustomAccessibilityAction(label) {
            val move = TimeGridLayout.move(event, day, minutes, zone, snap)
            if (move != null) onMove(move)
            move != null
        }
    }
    if (wall.minutes - snap >= range.start) add(context.text.moves.earlier, wall.day, wall.minutes - snap)
    if (wall.minutes + snap + duration <= range.end) add(context.text.moves.later, wall.day, wall.minutes + snap)
    if (index > 0) add(context.text.moves.previousDay, context.days[index - 1], wall.minutes)
    if (index >= 0 && index < context.days.lastIndex) {
        add(context.text.moves.nextDay, context.days[index + 1], wall.minutes)
    }
    return actions
}
