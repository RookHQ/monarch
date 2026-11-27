package com.rookhq.monarch.components.intelligence.aitraceview

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.tokens.MonarchTokens

/** The totals of a trace. Costs are per currency, in the order the currencies first appear. */
internal data class MonarchAiTraceTotals(
    val start: Long,
    val end: Long,
    val inputTokens: Long,
    val outputTokens: Long,
    val tokensKnown: Boolean,
    val costs: List<Pair<String, Long>>,
    val errorCount: Int,
) {
    val duration: Long get() = end - start
    val totalTokens: Long get() = inputTokens + outputTokens
}

/** One visible row of the trace. */
internal data class MonarchAiTraceLine(
    val span: MonarchAiTraceSpan,
    val depth: Int,
    val hasChildren: Boolean,
    val open: Boolean,
)

/** Component tokens and the pure logic of MonarchAiTraceView. */
internal object MonarchAiTraceViewTokens {
    /** The gap between the parts of the trace. */
    val Gap: Dp = MonarchTokens.Space.space3.dp

    /** The gap inside a row and between totals. */
    val SmallGap: Dp = MonarchTokens.Space.space1.dp

    /** The indent of each level. */
    val SpanWidth: Dp = (MonarchTokens.Space.space24 * 2).dp
    val KindWidth: Dp = MonarchTokens.Space.space24.dp
    val NumberWidth: Dp = MonarchTokens.Space.space20.dp
    val CostWidth: Dp = MonarchTokens.Space.space24.dp
    val TimelineWidth: Dp = (MonarchTokens.Space.space24 * 2).dp
    val TableWidth: Dp = SpanWidth + KindWidth + NumberWidth * 2 + CostWidth + TimelineWidth
    val Indent: Dp = MonarchTokens.Space.space4.dp

    /** The padding above and below a row's text, and at its end. */
    val RowPadding: Dp = MonarchTokens.Space.space2.dp

    /** The height of a bar. */
    val BarHeight: Dp = MonarchTokens.Space.space1_5.dp

    /** The shortest a bar is drawn, so an instant span stays visible. */
    val MinimumBar: Dp = MonarchTokens.Space.space0_5.dp

    /** The turn of the chevron of an open span, in degrees. */
    const val EXPANDED_ROTATION: Float = 90f

    /** Totals over the spans: wall-clock time to [now] while a span runs, tokens, cost per currency and errors. */
    fun totals(spans: List<MonarchAiTraceSpan>, now: Long?, currency: String): MonarchAiTraceTotals {
        val start = spans.minOfOrNull { it.start } ?: 0L
        var end = spans.maxOfOrNull { maxOf(it.start, it.end ?: it.start) } ?: start
        if (now != null && spans.any { it.end == null }) end = maxOf(end, now)
        val costs = linkedMapOf<String, Long>()
        spans.forEach { span ->
            span.costMicros?.let { costs[span.currency ?: currency] = (costs[span.currency ?: currency] ?: 0L) + it }
        }
        return MonarchAiTraceTotals(
            start = start,
            end = maxOf(end, start),
            inputTokens = spans.sumOf { it.inputTokens ?: 0L },
            outputTokens = spans.sumOf { it.outputTokens ?: 0L },
            tokensKnown = spans.any { it.inputTokens != null || it.outputTokens != null },
            costs = costs.toList(),
            errorCount = spans.count { it.status == MonarchAiTraceSpanStatus.Error },
        )
    }

    /** The spans by id; a repeated id keeps the first span. */
    private fun byId(spans: List<MonarchAiTraceSpan>): Map<String, MonarchAiTraceSpan> {
        val map = linkedMapOf<String, MonarchAiTraceSpan>()
        spans.forEach { map.putIfAbsent(it.id, it) }
        return map
    }

    /** The span [span] ran inside, or `null` at the top level, for an unknown parent and in a parent loop. */
    private fun parentOf(
        span: MonarchAiTraceSpan,
        spans: Map<String, MonarchAiTraceSpan>,
        cyclic: Set<String>,
    ): String? = span.parentId?.takeIf { it != span.id && it in spans && span.id !in cyclic }

    /** The ids of spans whose parents loop back to them; each is treated as top level. */
    private fun cyclic(spans: Map<String, MonarchAiTraceSpan>): Set<String> {
        val out = mutableSetOf<String>()
        val visited = mutableSetOf<String>()
        spans.values.forEach { first ->
            val path = mutableListOf<String>()
            var at: MonarchAiTraceSpan? = first
            while (at != null && at.id !in visited) {
                visited += at.id
                path += at.id
                at = at.parentId?.let { spans[it] }
            }
            val from = at?.let { path.indexOf(it.id) } ?: -1
            if (from >= 0) out += path.drop(from)
        }
        return out
    }

    /** Children by parent id (`null` for the top level), in start order. */
    fun children(spans: List<MonarchAiTraceSpan>): Map<String?, List<MonarchAiTraceSpan>> {
        val map = byId(spans)
        val loops = cyclic(map)
        return map.values.groupBy { parentOf(it, map, loops) }.mapValues { (_, list) -> list.sortedBy { it.start } }
    }

    /** The spans open at first: every top-level span, every failed span and every ancestor of one. */
    fun defaultExpanded(spans: List<MonarchAiTraceSpan>): Set<String> {
        val map = byId(spans)
        val loops = cyclic(map)
        val out = mutableSetOf<String>()
        map.values.forEach { span ->
            if (parentOf(span, map, loops) == null) out += span.id
            if (span.status == MonarchAiTraceSpanStatus.Error) out += ancestors(span, map)
        }
        return out
    }

    private fun ancestors(span: MonarchAiTraceSpan, map: Map<String, MonarchAiTraceSpan>): Set<String> {
        val out = linkedSetOf<String>()
        var at: MonarchAiTraceSpan? = span
        while (at != null && out.add(at.id)) at = at.parentId?.let { map[it] }
        return out
    }

    /** How long a span took; a running span lasts until [runEnd]. */
    fun duration(span: MonarchAiTraceSpan, runEnd: Long): Long = ((span.end ?: runEnd) - span.start).coerceAtLeast(0L)

    /** The rows shown under [filter] with [expanded] spans open. */
    fun lines(
        spans: List<MonarchAiTraceSpan>,
        filter: MonarchAiTraceFilter,
        expanded: Set<String>,
        runEnd: Long,
    ): List<MonarchAiTraceLine> {
        val children = children(spans)
        val keep: Set<String>? = if (filter == MonarchAiTraceFilter.Errors) {
            val map = byId(spans)
            map.values.filter { it.status == MonarchAiTraceSpanStatus.Error }.flatMap { ancestors(it, map) }.toSet()
        } else {
            null
        }
        fun kids(id: String?): List<MonarchAiTraceSpan> {
            var list = children[id].orEmpty()
            if (keep != null) list = list.filter { it.id in keep }
            if (filter == MonarchAiTraceFilter.Slowest) list = list.sortedByDescending { duration(it, runEnd) }
            return list
        }
        val out = mutableListOf<MonarchAiTraceLine>()
        fun walk(id: String?, depth: Int) {
            kids(id).forEach { span ->
                val hasChildren = kids(span.id).isNotEmpty()
                val open = hasChildren && span.id in expanded
                out += MonarchAiTraceLine(span, depth, hasChildren, open)
                if (open) walk(span.id, depth + 1)
            }
        }
        walk(null, 0)
        return out
    }

    /** Where a span's bar starts and how long it is, as fractions of the run. */
    fun bar(span: MonarchAiTraceSpan, totals: MonarchAiTraceTotals): Pair<Float, Float> {
        val total = totals.duration.coerceAtLeast(1L).toFloat()
        val start = ((span.start - totals.start) / total).coerceIn(0f, 1f)
        val length = (duration(span, totals.end) / total).coerceIn(0f, 1f - start)
        return start to length
    }

    /** [expanded] with [id] opened or closed. */
    fun toggled(expanded: Set<String>, id: String): Set<String> = if (id in expanded) expanded - id else expanded + id

    /** Whether Show in conversation is offered for a span. */
    fun opensPart(span: MonarchAiTraceSpan): Boolean = span.partId != null || span.messageId != null
}
