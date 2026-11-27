package com.rookhq.monarch.sample.sections.charts

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.charts.calendarheatmap.MonarchCalendarHeatmap
import com.rookhq.monarch.components.charts.calendarheatmap.MonarchCalendarHeatmapDay
import com.rookhq.monarch.components.charts.calendarheatmap.MonarchCalendarHeatmapLayout
import com.rookhq.monarch.components.forms.segmentedcontrol.MonarchSegmentedControl
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

/** The catalogue page of CalendarHeatmap. */
object MonarchCalendarHeatmapSection : CataloguePage {
    override val title = "CalendarHeatmap"

    private const val DAY = 86_400_000L

    /** 2026-01-01. */
    private const val START = 20_454L * DAY
    private const val DAYS = 181
    private const val WEEK = 7
    private const val SATURDAY = 6
    private const val THURSDAY = 4
    private const val SPREAD = 13
    private const val HOLIDAY = 18
    private const val MONTH = 30

    private val days = (0 until DAYS).map { index ->
        val weekday = (index + THURSDAY) % WEEK
        val weekend = weekday == 0 || weekday == SATURDAY
        MonarchCalendarHeatmapDay(
            date = START + index * DAY,
            value = if (weekend) null else (index * WEEK % SPREAD).toDouble(),
            note = if (index == HOLIDAY) "Holiday" else null,
            inactive = weekend,
            marker = index == HOLIDAY,
        )
    }

    @Composable
    override fun Content() {
        var layout by rememberSaveable { mutableIntStateOf(0) }
        var opened by rememberSaveable { mutableStateOf("") }
        CatalogueCaption("Layout")
        MonarchSegmentedControl {
            MonarchCalendarHeatmapLayout.entries.forEachIndexed { index, option ->
                Segment(
                    selected = index == layout,
                    onClick = { layout = index },
                    label = { Text(option.name) },
                )
            }
        }
        val month = layout == 1
        MonarchCalendarHeatmap(
            title = if (month) "Commits in January" else "Commits",
            days = days,
            from = START,
            to = if (month) START + MONTH * DAY else null,
            description = if (month) "January 2026" else "January to June 2026",
            layout = MonarchCalendarHeatmapLayout.entries[layout],
            today = START + (DAYS - 1) * DAY,
            footer = if (opened.isEmpty()) "Tap a day" else "Opened $opened",
            onSelect = { day -> opened = day.value?.toInt()?.toString() ?: "a day without data" },
        )
    }
}
