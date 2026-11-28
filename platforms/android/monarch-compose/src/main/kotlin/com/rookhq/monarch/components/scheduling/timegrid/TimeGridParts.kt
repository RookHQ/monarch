package com.rookhq.monarch.components.scheduling.timegrid

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.accessibility.MonarchTouchTarget
import com.rookhq.monarch.accessibility.monarchFocusRing
import com.rookhq.monarch.components.scheduling.agendalist.MonarchAgendaEvent
import com.rookhq.monarch.components.scheduling.eventchip.MonarchEventChip
import com.rookhq.monarch.components.scheduling.eventchip.MonarchEventChipVariant
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.theme.MonarchTheme
import com.rookhq.monarch.tokens.MonarchTokens

/** The resolved built-in strings of a time grid. */
internal class TimeGridText(
    val allDay: String,
    val createEvent: (String) -> String,
    val today: String,
    val openDay: (String) -> String,
    val range: (String, String) -> String,
    val until: (String) -> String,
    val moves: TimeGridMoveText,
)

/** The resolved names of the TalkBack move actions. */
internal class TimeGridMoveText(
    val earlier: String,
    val later: String,
    val previousDay: String,
    val nextDay: String,
)

/** A move in progress: where the ghost of [event] is drawn. */
internal data class TimeGridDrag(val event: MonarchAgendaEvent, val duration: Int, val dayIndex: Int, val start: Int)

/**
 * The heading of one day: the short weekday over the date, the date on a
 * filled circle for today. One element read as the long date (with "Today"),
 * a heading, or a button that opens the day when [onClick] is set.
 */
@Composable
internal fun TimeGridHeading(
    day: Long,
    width: Dp,
    today: Boolean,
    formats: TimeGridFormats,
    text: TimeGridText,
    colors: TimeGridColors,
    onClick: (() -> Unit)?,
) {
    val tokens = MonarchTimeGridTokens
    val longDay = formats.longDay(day)
    val description = tokens.headingDescription(longDay, text.today.takeIf { today })
    val interaction = if (onClick != null) {
        Modifier
            .monarchFocusRing(MonarchShapeDefaults.Control)
            .clickable(onClickLabel = text.openDay(longDay), role = Role.Button, onClick = onClick)
    } else {
        Modifier
    }
    Box(
        modifier = Modifier
            .width(width)
            .heightIn(min = MonarchTouchTarget.Minimum)
            .then(interaction)
            .semantics {
                contentDescription = description
                if (onClick == null) heading()
            }
            .padding(vertical = tokens.BlockPadding),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.clearAndSetSemantics {},
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(tokens.HeadingGap),
        ) {
            Text(
                text = formats.weekday(day),
                style = MonarchTheme.typography.caption,
                color = colors.secondaryText,
                textAlign = TextAlign.Center,
            )
            Box(
                modifier = Modifier
                    .sizeIn(minWidth = tokens.TodaySize, minHeight = tokens.TodaySize)
                    .then(if (today) Modifier.background(colors.todayFill, CircleShape) else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = formats.dayNumber(day),
                    style = MonarchTheme.typography.label,
                    fontWeight = FontWeight(MonarchTokens.FontWeight.semibold),
                    color = if (today) colors.todayText else colors.dateText,
                )
            }
        }
    }
}

/** The all-day events of one day, as bar chips under one another. */
@Composable
internal fun TimeGridAllDayCell(
    events: List<MonarchAgendaEvent>,
    width: Dp,
    text: TimeGridText,
    onSelectEvent: ((MonarchAgendaEvent) -> Unit)?,
) {
    val tokens = MonarchTimeGridTokens
    Column(
        modifier = Modifier
            .width(width)
            .padding(horizontal = tokens.EventGap, vertical = tokens.HeadingGap),
        verticalArrangement = Arrangement.spacedBy(tokens.HeadingGap),
    ) {
        events.forEach { event ->
            MonarchEventChip(
                title = event.title,
                modifier = Modifier.fillMaxWidth(),
                variant = MonarchEventChipVariant.Bar,
                status = event.status,
                color = event.color,
                timeLabel = text.allDay,
                onClick = onSelectEvent?.let { select -> { select(event) } },
            )
        }
    }
}

/**
 * The hour labels beside the grid, each centred on its hour line; the first
 * hour has no label because the line is the top edge. Hidden from TalkBack:
 * slots and events say their times.
 */
@Composable
internal fun TimeGridGutter(
    hours: List<Int>,
    hourHeight: Dp,
    width: Dp,
    formats: TimeGridFormats,
    colors: TimeGridColors,
) {
    val padding = MonarchTimeGridTokens.GutterPadding
    Layout(
        modifier = Modifier
            .width(width)
            .clearAndSetSemantics {},
        content = {
            hours.drop(1).forEach { hour ->
                Text(
                    text = formats.minutes(hour * TimeGridModel.MINUTES_PER_HOUR),
                    modifier = Modifier.padding(horizontal = padding),
                    style = MonarchTheme.typography.caption,
                    color = colors.secondaryText,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                )
            }
        },
    ) { measurables, constraints ->
        val hourPx = hourHeight.roundToPx()
        val height = hourPx * hours.size
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        layout(constraints.maxWidth, height) {
            placeables.forEachIndexed { index, placeable ->
                val line = (index + 1) * hourPx
                val y = (line - placeable.height / 2).coerceIn(0, maxOf(0, height - placeable.height))
                placeable.placeRelative(constraints.maxWidth - placeable.width, y)
            }
        }
    }
}

/** A block of time drawn hatched with its label, read as "label, start to end". */
@Composable
internal fun TimeGridUnavailableBlock(
    description: String,
    label: String?,
    top: Dp,
    height: Dp,
    colors: TimeGridColors,
) {
    val tokens = MonarchTimeGridTokens
    // The block is as tall as the time it covers; a label longer than a short block is clipped.
    Box(
        modifier = Modifier
            .padding(top = top, end = tokens.ColumnEndInset)
            .fillMaxWidth()
            .height(height)
            .clipToBounds()
            .background(colors.unavailableFill)
            .drawBehind { drawHatch(colors.unavailableHatch, tokens.HatchSpacing.toPx(), tokens.LineWidth.toPx()) }
            .semantics(mergeDescendants = true) { contentDescription = description },
    ) {
        if (label != null) {
            Text(
                text = label,
                modifier = Modifier
                    .padding(tokens.BlockPadding)
                    .clearAndSetSemantics {},
                style = MonarchTheme.typography.caption,
                color = colors.unavailableText,
            )
        }
    }
}

/** Diagonal hatch lines over the whole area, the same in both directions. */
private fun DrawScope.drawHatch(color: Color, spacing: Float, stroke: Float) {
    var x = -size.height
    while (x < size.width) {
        drawLine(color, Offset(x, size.height), Offset(x + size.height, 0f), strokeWidth = stroke)
        x += spacing
    }
}

/** Grid lines of the day columns: a line at every hour and a separator at the end edge of each column. */
internal fun DrawScope.drawTimeGridLines(
    hourPx: Float,
    hours: Int,
    color: Color,
    stroke: Float,
) {
    for (index in 1 until hours) {
        val y = index * hourPx
        drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = stroke)
    }
    val x = if (layoutDirection == LayoutDirection.Ltr) size.width - stroke / 2 else stroke / 2
    drawLine(color, Offset(x, 0f), Offset(x, size.height), strokeWidth = stroke)
}

/** The now line across a column, with a dot at its start edge. Hidden from TalkBack. */
@Composable
internal fun TimeGridNowLine(top: Dp, color: Color) {
    val tokens = MonarchTimeGridTokens
    Box(
        modifier = Modifier
            .padding(top = maxOf(Dp.Hairline, top - tokens.NowDotSize / 2))
            .fillMaxWidth()
            .clearAndSetSemantics {},
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = tokens.NowLineWidth, max = tokens.NowLineWidth)
                .background(color),
        )
        Box(modifier = Modifier.size(tokens.NowDotSize).background(color, CircleShape))
    }
}
