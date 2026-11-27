package com.rookhq.monarch.components.scheduling.agendalist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.R
import com.rookhq.monarch.components.datadisplay.badge.MonarchBadge
import com.rookhq.monarch.components.feedback.emptystate.MonarchEmptyState
import com.rookhq.monarch.components.layout.divider.MonarchDivider
import com.rookhq.monarch.components.layout.divider.MonarchDividerVariant
import com.rookhq.monarch.components.scheduling.eventchip.MonarchEventChip
import com.rookhq.monarch.components.scheduling.eventchip.MonarchEventChipVariant
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.internal.monarchTimeZone
import com.rookhq.monarch.theme.MonarchTheme
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Events grouped by day in a scrolling list: a pinned heading per day with
 * the date, a Today marker and the number of events, all-day events first,
 * events that last several days on each day they touch, and an empty state
 * when nothing is scheduled.
 *
 * Built by Monarch on a Compose `LazyColumn` with a `stickyHeader` per day;
 * each event is a `MonarchEventChip` in its bar form. Give the list a bounded
 * height (a screen's content, `Modifier.weight` or `Modifier.height`), as for
 * any `LazyColumn`. Days are dates in [timeZone]; days and times are
 * formatted in the locale of the configuration.
 *
 * @param events the events in any order.
 * @param modifier the modifier of the list.
 * @param from the first day listed, any time on that day in epoch milliseconds. `null` starts at the first day
 * with an event. Events that started earlier and still run appear on it.
 * @param days how many days from [from] to list. `null` lists through the last event. At most 400 days are listed.
 * @param today any time on the day marked Today, in epoch milliseconds, or `null` for no marker. Pass a fixed
 * value in tests and previews.
 * @param showEmptyDays lists the days in the range that have no events and no notes.
 * @param timeZone the zone days and times are read in. An IANA id such as `"Europe/Berlin"`. `null`, or an id the
 * device does not know, is the device's zone.
 * @param dayNotes informational lines shown first in a day, such as time off or a holiday, keyed by any time on
 * that day. A day with notes is listed even without events.
 * @param state the scroll state of the list.
 * @param contentPadding padding around the list's content, for example for system bars.
 * @param labels replacements for the built-in strings.
 * @param onSelect opens an event. With it each event is a button; without it the events are labels.
 */
@Suppress("LongParameterList")
@Composable
public fun MonarchAgendaList(
    events: List<MonarchAgendaEvent>,
    modifier: Modifier = Modifier,
    from: Long? = null,
    days: Int? = null,
    today: Long? = System.currentTimeMillis(),
    showEmptyDays: Boolean = false,
    timeZone: String? = null,
    dayNotes: Map<Long, List<String>> = emptyMap(),
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(),
    labels: MonarchAgendaListLabels = MonarchAgendaListLabels(),
    onSelect: ((MonarchAgendaEvent) -> Unit)? = null,
) {
    val zone = monarchTimeZone(timeZone)
    val rows = remember(events, from, days, zone, dayNotes, showEmptyDays) {
        val range = AgendaListDays.range(events, from, days, zone)
        AgendaListDays.days(events, range, zone, dayNotes, showEmptyDays)
    }
    if (rows.isEmpty()) {
        Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            MonarchEmptyState(
                title = monarchString(labels.emptyTitle, R.string.monarch_agendalist_empty_title),
            )
        }
        return
    }
    val locale = LocalConfiguration.current.locales[0]
    val formats = remember(locale, zone) { AgendaListFormats(locale, zone) }
    val todayKey = today?.let { AgendaListDays.dayOf(it, zone) }
    val text = AgendaListText(
        today = monarchString(labels.today, R.string.monarch_agendalist_today),
        allDay = monarchString(labels.allDay, R.string.monarch_agendalist_all_day),
        until = labels.until ?: stringResource(R.string.monarch_agendalist_until).let { f ->
            { end: String -> f.format(end) }
        },
        range = labels.range ?: stringResource(R.string.monarch_agendalist_range).let { f ->
            { start: String, end: String -> f.format(start, end) }
        },
    )
    val counts = rows.associate { row ->
        row.day to (labels.count?.invoke(row.events.size) ?: countText(row.events.size))
    }
    LazyColumn(modifier = modifier.fillMaxWidth(), state = state, contentPadding = contentPadding) {
        rows.forEach { row ->
            stickyHeader(key = "day-${row.day}", contentType = "day") {
                DayHeading(
                    date = formats.day(row.day),
                    today = text.today.takeIf { row.day == todayKey },
                    count = counts.getValue(row.day),
                )
            }
            row.notes.forEachIndexed { index, note ->
                item(key = "note-${row.day}-$index", contentType = "note") { DayNote(note) }
            }
            row.events.forEachIndexed { index, event ->
                item(key = "event-${row.day}-${event.id}", contentType = "event") {
                    val time = AgendaListDays.time(
                        event, row.day, zone, text.allDay, text.range, text.until, formats::time,
                    )
                    AgendaItem(event, time, last = index == row.events.lastIndex, onSelect = onSelect)
                }
            }
        }
    }
}

/** The resolved built-in strings. */
private class AgendaListText(
    val today: String,
    val allDay: String,
    val until: (String) -> String,
    val range: (String, String) -> String,
)

@Composable
private fun countText(count: Int): String = pluralStringResource(R.plurals.monarch_agendalist_count, count, count)

/** A day heading, on the surface colour so it covers the events that scroll under it. Read as one heading. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DayHeading(date: String, today: String?, count: String) {
    val tokens = MonarchAgendaListTokens
    val colors = tokens.headingColors(MonarchTheme.colors)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.container)
            .semantics(mergeDescendants = true) {
                heading()
                contentDescription = tokens.headingDescription(date, today, count)
            },
    ) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = tokens.HeadingHorizontalPadding, vertical = tokens.HeadingVerticalPadding),
            horizontalArrangement = Arrangement.spacedBy(tokens.HeadingGap),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = date,
                style = MonarchTheme.typography.label,
                color = if (today != null) MonarchTheme.colors.accentStrong else colors.date,
            )
            if (today != null) MonarchBadge(text = today.uppercase(), tone = MonarchTone.Info)
            Text(text = count, style = MonarchTheme.typography.caption, color = colors.count)
        }
        MonarchDivider(modifier = Modifier.align(Alignment.BottomStart), variant = MonarchDividerVariant.Subtle)
    }
}

@Composable
private fun DayNote(note: String) {
    val tokens = MonarchAgendaListTokens
    val stripe = MonarchTheme.colors.borderSubtle
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = tokens.ItemHorizontalPadding, vertical = tokens.NoteVerticalPadding)
            .clip(MonarchTheme.shapes.sm)
            .drawBehind {
                val step = MonarchAgendaListTokens.HatchSpacing.toPx()
                val width = MonarchAgendaListTokens.HatchWidth.toPx()
                var x = -size.height
                while (x < size.width) {
                    drawLine(stripe, Offset(x, size.height), Offset(x + size.height, 0f), strokeWidth = width)
                    x += step
                }
            },
    ) {
        Text(
            text = note,
            modifier = Modifier.padding(
                horizontal = tokens.NoteVerticalPadding * 2,
                vertical = tokens.NoteVerticalPadding * 2,
            ),
            style = MonarchTheme.typography.caption,
            color = tokens.noteColor(MonarchTheme.colors),
        )
    }
}

@Composable
private fun AgendaItem(
    event: MonarchAgendaEvent,
    time: AgendaListTime,
    last: Boolean,
    onSelect: ((MonarchAgendaEvent) -> Unit)?,
) {
    val tokens = MonarchAgendaListTokens
    MonarchEventChip(
        title = event.title,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = tokens.ItemHorizontalPadding,
                end = tokens.ItemHorizontalPadding,
                top = tokens.ItemVerticalPadding,
                bottom = if (last) tokens.DayBottomPadding else tokens.ItemVerticalPadding,
            ),
        variant = MonarchEventChipVariant.Bar,
        status = event.status,
        color = event.color,
        time = time.visible,
        timeLabel = time.spoken,
        detail = event.detail,
        onClick = onSelect?.let { select -> { select(event) } },
        timeGutter = true,
    )
}

/** The day and time formats of the list in one locale and zone. */
private class AgendaListFormats(locale: Locale, zone: TimeZone) {
    private val dayFormat = android.icu.text.DateFormat.getInstanceForSkeleton(DAY, locale).apply {
        timeZone = android.icu.util.TimeZone.getTimeZone("UTC")
    }
    private val timeFormat = android.icu.text.DateFormat.getInstanceForSkeleton(TIME, locale).apply {
        timeZone = android.icu.util.TimeZone.getTimeZone(zone.id)
    }

    /** "Wednesday, November 4" for a day key. */
    fun day(day: Long): String = dayFormat.format(Date(day))

    /** "09:30" or "9:30 AM", as the locale writes a time. */
    fun time(millis: Long): String = timeFormat.format(Date(millis))

    private companion object {
        const val DAY = "EEEEMMMMd"
        const val TIME = "jmm"
    }
}
