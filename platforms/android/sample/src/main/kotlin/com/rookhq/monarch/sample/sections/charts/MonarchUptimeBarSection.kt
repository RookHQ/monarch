package com.rookhq.monarch.sample.sections.charts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.charts.uptimebar.MonarchUptimeBar
import com.rookhq.monarch.components.charts.uptimebar.MonarchUptimeBarDetail
import com.rookhq.monarch.components.charts.uptimebar.MonarchUptimeBarSegment
import com.rookhq.monarch.components.charts.uptimebar.MonarchUptimeBarSize
import com.rookhq.monarch.components.charts.uptimebar.MonarchUptimeBarStatus
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

/** The catalogue page of UptimeBar. */
object MonarchUptimeBarSection : CataloguePage {
    override val title = "UptimeBar"

    private const val DEGRADED_DAY = 12
    private const val DOWN_DAY = 24
    private const val MAINTENANCE_DAY = 7
    private const val DAYS = 30
    private const val TWO_WEEKS = 14
    private const val DEGRADED = 99.42
    private const val DOWN = 96.4
    private const val UP = 100.0

    private val days = (1..DAYS).map { day ->
        MonarchUptimeBarSegment(
            id = "2026-03-$day",
            label = "Mar $day",
            status = if (day == MAINTENANCE_DAY) MonarchUptimeBarStatus.Maintenance else null,
            uptime = when (day) {
                DEGRADED_DAY -> DEGRADED
                DOWN_DAY -> DOWN
                MAINTENANCE_DAY -> null
                else -> UP
            },
            details = listOf(MonarchUptimeBarDetail("Checks", "1,440")),
            note = if (day == DOWN_DAY) "Database failover" else null,
        )
    }

    @Composable
    override fun Content() {
        var opened by rememberSaveable { mutableStateOf("Tap a day, then Open") }
        CatalogueCaption("A status page bar")
        MonarchUptimeBar(
            segments = days,
            label = "Search API",
            description = "Last 30 days",
            startLabel = "30 days ago",
            endLabel = "Today",
            showLegend = true,
            onSegmentSelect = { opened = "Opened ${it.label}" },
        )
        MonarchText(opened)
        CatalogueCaption("Two small bars sharing one inspected day")
        var active by rememberSaveable { mutableStateOf<Int?>(null) }
        MonarchUptimeBar(
            segments = days.take(TWO_WEEKS),
            label = "API",
            size = MonarchUptimeBarSize.Sm,
            activeIndex = active,
            onActiveIndexChange = { active = it },
        )
        MonarchUptimeBar(
            segments = days.takeLast(TWO_WEEKS),
            label = "Website",
            size = MonarchUptimeBarSize.Sm,
            activeIndex = active,
            onActiveIndexChange = { active = it },
        )
    }
}
