package com.rookhq.monarch.components.scheduling.calendarmonthview

import android.icu.text.DateFormatSymbols
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.accessibility.monarchFocusRing
import com.rookhq.monarch.components.scheduling.agendalist.MonarchAgendaEvent
import com.rookhq.monarch.components.scheduling.eventchip.MonarchEventChip
import com.rookhq.monarch.components.scheduling.eventchip.MonarchEventChipVariant
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.internal.scheduling.MonarchMonthGrid
import com.rookhq.monarch.theme.MonarchTheme
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Names of months, weekdays, days and times in one locale; days in UTC, times in the view's zone. */
internal class CalendarMonthViewFormats(locale: Locale, zone: TimeZone, is24Hour: Boolean) {
    private val utc = android.icu.util.TimeZone.getTimeZone("UTC")
    private fun format(skeleton: String, locale: Locale, zone: android.icu.util.TimeZone = utc) =
        android.icu.text.DateFormat.getInstanceForSkeleton(skeleton, locale).apply { timeZone = zone }

    private val longDay = format("yMMMMEEEEd", locale)
    private val panelDay = format("MMMMEEEEd", locale)
    private val timeFormat =
        format(if (is24Hour) "Hmm" else "hmma", locale, android.icu.util.TimeZone.getTimeZone(zone.id))
    private val numbers = android.icu.text.NumberFormat.getIntegerInstance(locale).apply { isGroupingUsed = false }
    private val symbols = DateFormatSymbols(locale)
    private val narrowWeekdays = symbols.getWeekdays(DateFormatSymbols.STANDALONE, DateFormatSymbols.ABBREVIATED)
    private val shortWeekdays = symbols.getWeekdays(DateFormatSymbols.STANDALONE, DateFormatSymbols.ABBREVIATED)
    private val weekdays = symbols.getWeekdays(DateFormatSymbols.STANDALONE, DateFormatSymbols.WIDE)

    /** "Wednesday, November 4, 2026". */
    fun longDay(day: Long): String = longDay.format(Date(day))

    /** "Wednesday, November 4", the heading of the selected day's list. */
    fun panelDay(day: Long): String = panelDay.format(Date(day))

    /** The number in a cell: "4", as on the web, also on the first of a month. */
    fun cellNumber(day: Long): String {
        return numbers.format(MonarchMonthGrid.dayOfMonth(day).toLong())
    }

    /** "9:30 AM" or "09:30", following the person's 12- or 24-hour setting. */
    fun time(millis: Long): String = timeFormat.format(Date(millis))

    /** The weekday name of a column header: narrow ("W") or abbreviated ("Wed"). */
    fun weekdayLabel(day: Long, wide: Boolean): String =
        (if (wide) shortWeekdays else narrowWeekdays)[MonarchMonthGrid.dayOfWeek(day)].uppercase()

    /** The full weekday name, read by TalkBack for a column header. */
    fun weekdayName(day: Long): String = weekdays[MonarchMonthGrid.dayOfWeek(day)]
}

/** The resolved built-in strings of the view. */
internal class CalendarMonthViewText(
    val today: String,
    val day: (String, Int) -> String,
    val more: (Int) -> String,
    val moreShort: (Int) -> String,
    val noEvents: String,
    val allDay: String,
    val until: (String) -> String,
    val range: (String, String) -> String,
    val moves: List<Pair<String, Int>>,
)

/** One month page: the weekday header and the weeks, as a TalkBack grid. */
@Suppress("LongParameterList")
internal class CalendarMonthViewPage(
    val page: Int,
    val month: Long,
    val weeks: List<List<Long>>,
    val byDay: Map<Long, List<MonarchAgendaEvent>>,
    val cellWidth: Dp,
    val today: Long,
    val selectedDay: Long?,
    val maxEvents: Int,
    val zone: TimeZone,
    val dayState: ((Long) -> MonarchCalendarMonthViewDayState?)?,
    val formats: CalendarMonthViewFormats,
    val text: CalendarMonthViewText,
    val drag: CalendarMonthViewDrag,
    val onDayClick: ((Long) -> Unit)?,
    val chips: Boolean = false,
) {
    private val wide = chips || MonarchCalendarMonthViewTokens.isWide(cellWidth)

    @Composable
    fun Content() {
        val tokens = MonarchCalendarMonthViewTokens
        Column(
            modifier = Modifier
                .width(cellWidth * MonarchMonthGrid.DAYS_PER_WEEK)
                .border(tokens.GridLine, tokens.gridLine(MonarchTheme.colors))
                .semantics {
                    collectionInfo = CollectionInfo(rowCount = weeks.size + 1, columnCount = DAYS)
                },
        ) {
            Row {
                weeks.first().forEachIndexed { column, day -> WeekdayHeader(column, day) }
            }
            weeks.forEachIndexed { row, week ->
                Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                    week.forEachIndexed { column, day -> DayCell(row + 1, column, day) }
                }
            }
        }
    }

    @Composable
    private fun WeekdayHeader(column: Int, day: Long) {
        val name = formats.weekdayName(day)
        Box(
            modifier = Modifier
                .width(cellWidth)
                .padding(vertical = MonarchCalendarMonthViewTokens.WeekdayPadding)
                .clearAndSetSemantics {
                    contentDescription = name
                    collectionItemInfo = CollectionItemInfo(0, 1, column, 1)
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = formats.weekdayLabel(day, wide),
                style = MonarchTheme.typography.caption,
                color = MonarchTheme.colors.textSubtle,
            )
        }
    }

    @Composable
    private fun DayCell(row: Int, column: Int, day: Long) {
        val tokens = MonarchCalendarMonthViewTokens
        val theme = MonarchTheme.colors
        val events = byDay[day].orEmpty()
        val state = CalendarMonthViewCellState(
            inMonth = MonarchMonthGrid.monthIndex(day) == MonarchMonthGrid.monthIndex(month),
            today = day == today,
            selected = day == selectedDay,
            dropTarget = day == drag.target,
        )
        val colors = tokens.cellColors(state, theme)
        val info = dayState?.invoke(day)
        val name = tokens.dayName(
            dateAndCount = text.day(formats.longDay(day), events.size),
            today = text.today.takeIf { state.today },
            badge = info?.badge,
        )
        val lineColor = tokens.gridLine(theme)
        val hatchColor = tokens.hatch(theme)
        val click = onDayClick
        val interaction = if (click != null) {
            Modifier
                .monarchFocusRing(RectangleShape)
                .selectable(selected = state.selected, role = Role.Button, onClick = { click(day) })
        } else {
            Modifier
        }
        Box(
            modifier = Modifier
                .width(cellWidth)
                .heightIn(min = if (wide) tokens.WideCellMinHeight else tokens.CompactCellMinHeight)
                .fillMaxHeight()
                .onGloballyPositioned { drag.place(page, day, it.boundsInRoot()) }
                .background(colors.container)
                .drawBehind {
                    if (info?.hatched == true) drawHatch(hatchColor, tokens.HatchSpacing.toPx(), tokens.GridLine.toPx())
                    drawCellLines(lineColor, tokens.GridLine.toPx())
                    colors.outline?.let { outline ->
                        val width = tokens.DropOutline.toPx()
                        drawRect(
                            outline,
                            topLeft = Offset(width / 2, width / 2),
                            size = size.copy(width = size.width - width, height = size.height - width),
                            style = Stroke(width),
                        )
                    }
                }
                .then(interaction)
                .semantics(mergeDescendants = true) {
                    contentDescription = name
                    selected = state.selected
                    collectionItemInfo = CollectionItemInfo(row, 1, column, 1)
                },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(tokens.CellPadding)
                    .clearAndSetSemantics {},
                horizontalAlignment = if (wide) Alignment.Start else Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(tokens.CellGap),
            ) {
                NumberMark(day, state, colors)
                info?.badge?.takeIf { it.isNotBlank() }?.let { badge ->
                    Text(
                        text = badge,
                        style = MonarchTheme.typography.caption,
                        color = theme.textMuted,
                        textAlign = if (wide) TextAlign.Start else TextAlign.Center,
                    )
                }
                if (wide) WideEvents(day, events) else Dots(events)
            }
        }
    }

    @Composable
    private fun NumberMark(day: Long, state: CalendarMonthViewCellState, colors: CalendarMonthViewCellColors) {
        val tokens = MonarchCalendarMonthViewTokens
        val shape = MonarchShapeDefaults.Mark
        val ring = colors.markBorder?.let { Modifier.border(MonarchShapeDefaults.BorderWidth, it, shape) } ?: Modifier
        Box(
            modifier = Modifier
                .sizeIn(minWidth = tokens.MarkSize, minHeight = tokens.MarkSize)
                .background(colors.mark, shape)
                .then(ring)
                .padding(horizontal = tokens.CellPadding),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = formats.cellNumber(day),
                style = MonarchTheme.typography.caption,
                fontWeight = if (state.today || state.selected) FontWeight(tokens.TODAY_WEIGHT) else null,
                color = colors.number,
                textAlign = TextAlign.Center,
            )
        }
    }

    @Composable
    private fun Dots(events: List<MonarchAgendaEvent>) {
        val tokens = MonarchCalendarMonthViewTokens
        val theme = MonarchTheme.colors
        if (events.isEmpty()) return
        Row(
            horizontalArrangement = Arrangement.spacedBy(tokens.DotGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            events.take(tokens.MAX_DOTS).forEach { event ->
                val color = tokens.dotColor(event.color, event.status, theme)
                val hollow = tokens.isHollow(event.status)
                Box(
                    modifier = Modifier.size(tokens.DotSize).drawBehind {
                        val stroke = tokens.DotStroke.toPx()
                        if (hollow) {
                            drawCircle(color, radius = (size.minDimension - stroke) / 2, style = Stroke(stroke))
                        } else {
                            drawCircle(color)
                        }
                    },
                )
            }
            if (events.size > tokens.MAX_DOTS) {
                Text(
                    text = text.moreShort(events.size - tokens.MAX_DOTS),
                    style = MonarchTheme.typography.caption,
                    color = theme.textMuted,
                )
            }
        }
    }

    @Composable
    private fun WideEvents(day: Long, events: List<MonarchAgendaEvent>) {
        val shown = CalendarMonthViewEvents.shownCount(events.size, maxEvents)
        events.take(shown).forEach { event ->
            MonarchEventChip(
                title = event.title,
                variant = MonarchEventChipVariant.Dot,
                status = event.status,
                color = event.color,
                time = CalendarMonthViewEvents.cellTime(event, day, zone, formats::time),
            )
        }
        val hidden = events.size - shown
        if (hidden > 0) {
            Text(
                text = text.more(hidden),
                style = MonarchTheme.typography.caption,
                color = MonarchTheme.colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }

    private companion object {
        const val DAYS = MonarchMonthGrid.DAYS_PER_WEEK
    }
}

/** The lines at the top and the start edge of a cell; the grid's border closes the end and the bottom. */
private fun DrawScope.drawCellLines(color: Color, width: Float) {
    drawLine(color, Offset(0f, width / 2), Offset(size.width, width / 2), width)
    val x = if (layoutDirection == LayoutDirection.Rtl) size.width - width / 2 else width / 2
    drawLine(color, Offset(x, 0f), Offset(x, size.height), width)
}

/** Diagonal lines over the whole cell, [spacing] apart. */
private fun DrawScope.drawHatch(color: Color, spacing: Float, width: Float) {
    clipRect {
        var x = 0f
        val reach = size.width + size.height
        while (x < reach) {
            drawLine(color, Offset(x, 0f), Offset(x - size.height, size.height), width)
            x += spacing
        }
    }
}
