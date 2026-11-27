package com.rookhq.monarch.components.scheduling.timegrid

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.Dp
import com.rookhq.monarch.components.scheduling.agendalist.MonarchAgendaEvent

/**
 * The long press and drag that moves an event: a haptic on pick-up, a tick
 * at each step, the ghost through [TimeGridEventInput.onDrag], and the move
 * on release when the start changed. A new touch forgets the previous hold,
 * so only the release of a hold skips opening the event.
 */
internal fun Modifier.timeGridDrag(
    key: String,
    pointer: TimeGridPointer,
    holder: Array<LayoutCoordinates?>,
    haptics: HapticFeedback,
    latest: () -> TimeGridEventInput,
): Modifier =
    this
        .pointerInput(Unit) {
            // A new touch forgets the previous hold, so only the release of a hold skips opening.
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                pointer.held = false
            }
        }
        .pointerInput(key) {
            var grab = 0
            var current: TimeGridDrag? = null
            detectDragGesturesAfterLongPress(
                onDragStart = { offset ->
                    val input = latest()
                    val at = timeGridPointerAt(pointer.area, holder[0], offset, input)
                        ?: return@detectDragGesturesAfterLongPress
                    pointer.held = true
                    grab = at.second - input.placed.top
                    val duration = TimeGridLayout.durationMinutes(input.event, input.context.snap)
                    current = TimeGridDrag(input.event, duration, at.first, input.placed.top)
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    input.onDrag(current)
                },
                onDrag = { change, _ ->
                    change.consume()
                    val input = latest()
                    val drag = current ?: return@detectDragGesturesAfterLongPress
                    val at = timeGridPointerAt(pointer.area, holder[0], change.position, input)
                        ?: return@detectDragGesturesAfterLongPress
                    val snap = input.context.snap
                    val snapped = Math.round((at.second - grab).toFloat() / snap) * snap
                    val start = TimeGridLayout.clampStart(snapped, drag.duration, input.context.range, snap)
                    if (start != drag.start || at.first != drag.dayIndex) {
                        current = drag.copy(dayIndex = at.first, start = start)
                        haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                        input.onDrag(current)
                    }
                },
                onDragEnd = {
                    val input = latest()
                    val drag = current
                    current = null
                    input.onDrag(null)
                    if (drag != null) {
                        val target = input.context.days[drag.dayIndex]
                        TimeGridLayout.move(drag.event, target, drag.start, input.context.zone, input.context.snap)
                            ?.let { input.context.onMoveEvent?.invoke(it) }
                    }
                },
                onDragCancel = {
                    current = null
                    latest().onDrag(null)
                },
            )
        }

/** What an event's drag reads when it runs. */
internal class TimeGridEventInput(
    val event: MonarchAgendaEvent,
    val placed: TimeGridPlaced,
    val context: TimeGridContext,
    val rtl: Boolean,
    val onDrag: (TimeGridDrag?) -> Unit,
)

/**
 * The day index and wall-clock minutes under [offset], a position inside
 * the event at [event], read in the columns row [area]. Columns are laid
 * out from the start edge, so right to left counts from the right.
 */
private fun timeGridPointerAt(
    area: LayoutCoordinates?,
    event: LayoutCoordinates?,
    offset: Offset,
    input: TimeGridEventInput,
): Pair<Int, Int>? {
    if (area == null || event == null) return null
    if (!area.isAttached || !event.isAttached) return null
    val position = area.localPositionOf(event, offset)
    val widthPx = area.size.width.toFloat()
    val fromStart = if (input.rtl) widthPx - position.x else position.x
    val context = input.context
    val total = context.widths.fold(Dp.Hairline) { sum, width -> sum + width }
    val scale = if (total.value > 0f) widthPx / total.value else 0f
    var edge = 0f
    var day = context.widths.lastIndex
    for ((index, width) in context.widths.withIndex()) {
        edge += width.value * scale
        if (fromStart < edge) {
            day = index
            break
        }
    }
    val heightPx = area.size.height.toFloat()
    val hours = context.range.hours.size
    val minutes = if (heightPx > 0f) {
        context.range.start + (position.y / heightPx * hours * TimeGridModel.MINUTES_PER_HOUR).toInt()
    } else {
        context.range.start
    }
    return day.coerceAtLeast(0) to minutes
}
