package com.rookhq.monarch.components.scheduling.calendarmonthview

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.zIndex
import com.rookhq.monarch.components.scheduling.agendalist.MonarchAgendaEvent
import com.rookhq.monarch.components.scheduling.eventchip.MonarchEventChip
import com.rookhq.monarch.components.scheduling.eventchip.MonarchEventChipVariant
import com.rookhq.monarch.foundation.monarchShadow
import com.rookhq.monarch.theme.MonarchTheme
import java.util.TimeZone

/** Where an event chip of the selected day's list is in the root, updated on every layout. */
private class CalendarMonthViewOrigin {
    var value: Offset = Offset.Zero
}

/**
 * The selected day's events under the grid: a heading with the date, then
 * every event of the day as a `MonarchEventChip` bar, or "No events". With
 * [onEventMove] each event can be dragged onto a day of the grid after a
 * long press, and has TalkBack actions that move it by a day or a week.
 */
@Suppress("LongParameterList")
@Composable
internal fun CalendarMonthViewDayPanel(
    day: Long,
    events: List<MonarchAgendaEvent>,
    zone: TimeZone,
    formats: CalendarMonthViewFormats,
    text: CalendarMonthViewText,
    page: Int,
    drag: CalendarMonthViewDrag,
    onEventClick: ((MonarchAgendaEvent) -> Unit)?,
    onEventMove: ((MonarchAgendaEvent, Int) -> Unit)?,
) {
    val tokens = MonarchCalendarMonthViewTokens
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = tokens.PanelTopPadding),
        verticalArrangement = Arrangement.spacedBy(tokens.PanelGap),
    ) {
        Text(
            text = formats.panelDay(day),
            modifier = Modifier.semantics {
                heading()
                liveRegion = LiveRegionMode.Polite
            },
            style = MonarchTheme.typography.bodyStrong,
            color = MonarchTheme.colors.textDefault,
        )
        if (events.isEmpty()) {
            Text(text = text.noEvents, style = MonarchTheme.typography.body, color = MonarchTheme.colors.textMuted)
        }
        events.forEach { event ->
            val time = CalendarMonthViewEvents.time(
                event, day, zone, text.allDay, text.range, text.until, formats::time,
            )
            MonarchEventChip(
                title = event.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (onEventMove != null) {
                            Modifier.calendarMonthViewMovable(event, day, page, drag, text, onEventMove)
                        } else {
                            Modifier
                        },
                    ),
                variant = MonarchEventChipVariant.Bar,
                status = event.status,
                color = event.color,
                time = time.visible,
                timeLabel = time.spoken,
                detail = event.detail,
                onClick = onEventClick?.let { click -> { click(event) } },
            )
        }
    }
}

/**
 * Lets an event chip be dragged after a long press and dropped on a day of
 * the grid, raised with a shadow and with a haptic tick when it is picked
 * up, and adds the TalkBack
 * actions that move it without dragging.
 */
@Suppress("LongParameterList")
@Composable
private fun Modifier.calendarMonthViewMovable(
    event: MonarchAgendaEvent,
    day: Long,
    page: Int,
    drag: CalendarMonthViewDrag,
    text: CalendarMonthViewText,
    onEventMove: (MonarchAgendaEvent, Int) -> Unit,
): Modifier {
    val haptic = LocalHapticFeedback.current
    val origin = remember { CalendarMonthViewOrigin() }
    val currentDay by rememberUpdatedState(day)
    val currentPage by rememberUpdatedState(page)
    val move by rememberUpdatedState(onEventMove)
    val currentEvent by rememberUpdatedState(event)
    val dragging = drag.eventId == event.id
    return this
        .onGloballyPositioned { origin.value = it.positionInRoot() }
        .zIndex(if (dragging) 1f else 0f)
        .graphicsLayer {
            if (drag.eventId == event.id) {
                translationX = drag.offset.x
                translationY = drag.offset.y
            }
        }
        .then(if (dragging) Modifier.monarchShadow(MonarchTheme.elevation.md, MonarchTheme.shapes.sm) else Modifier)
        .pointerInput(event.id) {
            detectDragGesturesAfterLongPress(
                onDragStart = { position ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    drag.begin(currentEvent.id, origin.value + position, currentPage)
                },
                onDrag = { change, amount ->
                    change.consume()
                    drag.move(amount)
                },
                onDragEnd = {
                    val target = drag.end()
                    if (target != null && target != currentDay) {
                        move(currentEvent, drag.daysBetween(currentDay, target))
                    }
                },
                onDragCancel = { drag.cancel() },
            )
        }
        .semantics {
            customActions = text.moves.map { (label, days) ->
                CustomAccessibilityAction(label) {
                    move(currentEvent, days)
                    true
                }
            }
        }
}
