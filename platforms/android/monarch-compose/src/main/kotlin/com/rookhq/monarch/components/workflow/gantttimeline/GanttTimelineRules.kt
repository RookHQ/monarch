package com.rookhq.monarch.components.workflow.gantttimeline

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset

/** A span of whole days, as epoch day numbers, first day before or equal to the last. */
@Immutable
internal data class GanttTimelineSpan(val start: Long, val end: Long) {
    /** The number of days, counting both ends. */
    val days: Int get() = (end - start + 1).toInt()
}

/** One row of the chart and the list. */
internal sealed interface GanttTimelineLine {
    /** A group heading. */
    data class Group(val group: MonarchGanttTimelineGroup) : GanttTimelineLine

    /**
     * An item.
     *
     * @property depth how deep it is nested under parents.
     * @property children how many direct children it has.
     * @property rollup the span of its children when it has no dates of its own.
     */
    data class Item(
        val item: MonarchGanttTimelineItem,
        val depth: Int,
        val children: Int,
        val rollup: GanttTimelineSpan?,
    ) : GanttTimelineLine
}

/** The rows and the top-level items without dates that are listed apart. */
internal class GanttTimelineLines(val lines: List<GanttTimelineLine>, val unscheduled: List<MonarchGanttTimelineItem>)

/** A header tick: a day and whether its month label carries the year. */
@Immutable
internal data class GanttTimelineTick(val day: Long, val withYear: Boolean)

/** The month ticks and the day or week ticks of the header. */
internal class GanttTimelineTicks(val major: List<GanttTimelineTick>, val minor: List<Long>)

/** The pure rules of MonarchGanttTimeline: rows, the window, ticks, dependency paths and gestures. */
internal object GanttTimelineRules {
    private const val QUARTER_MONTHS = 3
    private const val EDGE_ZONES = 3
    private const val LEAD_DAYS = 7L
    private const val MONDAY = 1
    private const val EPOCH_MONDAY = 4L

    /** The span of two optional dates (day keys), or `null` without either. Reversed dates are swapped. */
    fun span(start: Long?, end: Long?): GanttTimelineSpan? {
        val first = (start ?: end)?.let(GanttTimelineDays::day) ?: return null
        val last = (end ?: start)?.let(GanttTimelineDays::day) ?: return null
        return if (last < first) GanttTimelineSpan(last, first) else GanttTimelineSpan(first, last)
    }

    /** Whether only one of the two dates is set: a one-day marker. */
    fun isPoint(dates: MonarchGanttTimelineDates): Boolean = (dates.start == null) != (dates.end == null)

    /**
     * Groups first with their items, then the items of no known group; children under their parents unless the
     * parent is collapsed. Top-level items without dates, children or a rollup go to the unscheduled list when
     * [undated] is [MonarchGanttTimelineUndated.List]. Cycles and unknown parents are ignored.
     */
    fun lines(
        items: List<MonarchGanttTimelineItem>,
        groups: List<MonarchGanttTimelineGroup>,
        collapsed: Set<String>,
        undated: MonarchGanttTimelineUndated,
    ): GanttTimelineLines {
        val builder = GanttTimelineLineBuilder(items, collapsed, undated == MonarchGanttTimelineUndated.List)
        val top = items.filter { builder.isTopLevel(it) }
        val groupIds = groups.mapTo(HashSet()) { it.id }
        for (group in groups) {
            builder.out += GanttTimelineLine.Group(group)
            top.filter { it.groupId == group.id }.forEach { builder.visit(it, 0, setOf(it.id)) }
        }
        top.filter { it.groupId == null || it.groupId !in groupIds }.forEach { builder.visit(it, 0, setOf(it.id)) }
        return GanttTimelineLines(builder.out, builder.unscheduled)
    }

    /**
     * The first visible day: [start] when set, otherwise the Monday a week before [today], otherwise before the
     * earliest item, otherwise the first Monday of 1970.
     */
    fun anchor(start: Long?, today: Long?, items: List<MonarchGanttTimelineItem>): Long {
        if (start != null) return GanttTimelineDays.day(start)
        val base = today ?: items.mapNotNull { span(it.start, it.end)?.start }.minOrNull()
        return if (base != null) GanttTimelineDays.mondayOf(base - LEAD_DAYS) else EPOCH_MONDAY
    }

    /** Today's window: the Monday a week before [today]. */
    fun todayAnchor(today: Long): Long = GanttTimelineDays.mondayOf(today - LEAD_DAYS)

    /** The month ticks (every month, every third month at Year) and the day (Week) or Monday (Month) ticks. */
    fun ticks(anchor: Long, zoom: MonarchGanttTimelineZoom): GanttTimelineTicks {
        val major = ArrayList<GanttTimelineTick>()
        val minor = ArrayList<Long>()
        for (d in 0 until MonarchGanttTimelineTokens.span(zoom)) {
            val day = anchor + d
            val (dayOfMonth, month) = GanttTimelineDays.dayAndMonth(day)
            val first = d == 0
            val quarterly = zoom == MonarchGanttTimelineZoom.Year
            val shown = first || (dayOfMonth == 1 && (!quarterly || month % QUARTER_MONTHS == 0))
            if (shown) major += GanttTimelineTick(day, withYear = first || month == 0 || yearly(zoom))
            when (zoom) {
                MonarchGanttTimelineZoom.Week -> minor += day
                MonarchGanttTimelineZoom.Month -> if (GanttTimelineDays.weekday(day) == MONDAY) minor += day
                else -> Unit
            }
        }
        return GanttTimelineTicks(major, minor)
    }

    /** Whether every month label carries the year: at Quarter and Year zoom. */
    private fun yearly(zoom: MonarchGanttTimelineZoom): Boolean =
        zoom == MonarchGanttTimelineZoom.Quarter || zoom == MonarchGanttTimelineZoom.Year

    /**
     * The polyline of a dependency arrow in pixels, left to right, ending at the arrow's tip. [gap] is how far the
     * line leaves a bar before turning, [notch] the short step out of a bar that is too close, [tip] the space before
     * the target bar.
     */
    @Suppress("LongParameterList")
    fun dependencyPath(
        kind: MonarchGanttTimelineDependencyKind,
        from: GanttTimelineSpan,
        to: GanttTimelineSpan,
        y1: Float,
        y2: Float,
        x: (Long) -> Float,
        dayWidth: Float,
        gap: Float,
        tip: Float,
    ): List<Offset> {
        val startToStart = kind == MonarchGanttTimelineDependencyKind.StartToStart
        val fromX = if (startToStart) x(from.start) else x(from.end) + dayWidth
        val toX = if (kind == MonarchGanttTimelineDependencyKind.FinishToFinish) x(to.end) + dayWidth else x(to.start)
        if (kind == MonarchGanttTimelineDependencyKind.FinishToFinish) {
            val mid = maxOf(fromX, toX) + gap
            return listOf(Offset(fromX, y1), Offset(mid, y1), Offset(mid, y2), Offset(toX + tip, y2))
        }
        val mid = minOf(fromX + gap, toX - gap)
        if (mid > fromX) return listOf(Offset(fromX, y1), Offset(mid, y1), Offset(mid, y2), Offset(toX - tip, y2))
        val half = (y1 + y2) / 2
        val back = toX - gap - tip
        return listOf(
            Offset(fromX, y1),
            Offset(fromX + gap, y1),
            Offset(fromX + gap, half),
            Offset(back, half),
            Offset(back, y2),
            Offset(toX - tip, y2),
        )
    }

    /** Which dates a drag that starts [x] pixels into a bar [width] wide changes: the ends, or the whole bar. */
    fun dragShift(x: Float, width: Float, edge: Float, point: Boolean): MonarchGanttTimelineShift = when {
        point || width < edge * EDGE_ZONES -> MonarchGanttTimelineShift.Move
        x < edge -> MonarchGanttTimelineShift.Start
        x > width - edge -> MonarchGanttTimelineShift.End
        else -> MonarchGanttTimelineShift.Move
    }

    /** Whole days a drag of [pixels] covers. */
    fun dragDays(pixels: Float, dayWidth: Float): Int = Math.round(pixels / dayWidth)
}

/** Walks the items into rows for [GanttTimelineRules.lines]. */
private class GanttTimelineLineBuilder(
    items: List<MonarchGanttTimelineItem>,
    private val collapsed: Set<String>,
    private val listUndated: Boolean,
) {
    private val ids = items.mapTo(HashSet()) { it.id }
    private val children = items.filter { !isTopLevel(it) }.groupBy { checkNotNull(it.parentId) }
    val out = ArrayList<GanttTimelineLine>()
    val unscheduled = ArrayList<MonarchGanttTimelineItem>()

    /** An item without a known parent other than itself. */
    fun isTopLevel(item: MonarchGanttTimelineItem): Boolean =
        item.parentId == null || item.parentId !in ids || item.parentId == item.id

    /** The span of the children of [id] (and theirs, for children without dates), skipping [seen] items. */
    fun rollup(id: String, seen: Set<String>): GanttTimelineSpan? {
        val spans = children[id].orEmpty()
            .filter { it.id !in seen }
            .mapNotNull { GanttTimelineRules.span(it.start, it.end) ?: rollup(it.id, seen + it.id) }
        if (spans.isEmpty()) return null
        return GanttTimelineSpan(spans.minOf { it.start }, spans.maxOf { it.end })
    }

    /** Adds [item] at [depth] and, unless it is collapsed, its children; [seen] guards against cycles. */
    fun visit(item: MonarchGanttTimelineItem, depth: Int, seen: Set<String>) {
        val kids = children[item.id].orEmpty()
        val own = GanttTimelineRules.span(item.start, item.end)
        val roll = if (own == null) rollup(item.id, setOf(item.id)) else null
        val bare = own == null && roll == null && kids.isEmpty()
        if (bare && depth == 0 && listUndated) {
            unscheduled += item
            return
        }
        out += GanttTimelineLine.Item(item, depth, kids.size, roll)
        if (item.id in collapsed) return
        kids.filter { it.id !in seen }.forEach { visit(it, depth + 1, seen + it.id) }
    }
}
