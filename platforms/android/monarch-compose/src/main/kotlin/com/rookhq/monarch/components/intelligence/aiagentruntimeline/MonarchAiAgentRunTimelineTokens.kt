package com.rookhq.monarch.components.intelligence.aiagentruntimeline

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Where one event sits on the time axis, as fractions of the run. */
internal data class MonarchAiAgentRunTimelineSpan(val start: Float, val length: Float)

/** The totals of a run. Token and cost totals are `null` when no event knows them. */
internal data class MonarchAiAgentRunTimelineTotals(
    val durationMillis: Long,
    val inputTokens: Long?,
    val outputTokens: Long?,
    val costMicros: Long?,
)

/** Component tokens and the pure logic of MonarchAiAgentRunTimeline. */
internal object MonarchAiAgentRunTimelineTokens {
    /** The gap between the parts of the trace. */
    val Gap: Dp = MonarchTokens.Space.space3.dp

    /** The gap inside a row and between totals. */
    val SmallGap: Dp = MonarchTokens.Space.space2.dp

    /** The height of a bar. */
    val BarHeight: Dp = MonarchTokens.Space.space2.dp

    /** The shortest a bar is drawn, so an instant event stays visible. */
    val MinimumBar: Dp = MonarchTokens.Space.space0_5.dp

    /** The width of a table column before the font scale. */
    val ColumnWidth: Dp = MonarchTokens.Space.space24.dp

    /** The width of the table's event column before the font scale. */
    val NameColumnWidth: Dp = (MonarchTokens.Space.space24 + MonarchTokens.Space.space16).dp


    /** The events by start; equal starts keep their order. */
    fun sorted(events: List<MonarchAiRunEvent>): List<MonarchAiRunEvent> = events.sortedBy { it.startedAt }

    /** The end of the run: [now], else the latest start or end, so the trace never reads the clock. */
    fun runEnd(events: List<MonarchAiRunEvent>, now: Long?): Long =
        now ?: events.maxOfOrNull { maxOf(it.startedAt, it.endedAt ?: it.startedAt) } ?: 0L

    /** The start of the run. */
    fun runStart(events: List<MonarchAiRunEvent>): Long = events.minOfOrNull { it.startedAt } ?: 0L

    /** How long an event took; a running event lasts until [runEnd]; an end before the start counts as zero. */
    fun duration(event: MonarchAiRunEvent, runEnd: Long): Long =
        ((event.endedAt ?: maxOf(runEnd, event.startedAt)) - event.startedAt).coerceAtLeast(0L)

    /** Where an event sits on the axis from [runStart] to [runEnd]. */
    fun span(event: MonarchAiRunEvent, runStart: Long, runEnd: Long): MonarchAiAgentRunTimelineSpan {
        val total = (runEnd - runStart).coerceAtLeast(1L).toFloat()
        val start = ((event.startedAt - runStart) / total).coerceIn(0f, 1f)
        val length = (duration(event, runEnd) / total).coerceIn(0f, 1f - start)
        return MonarchAiAgentRunTimelineSpan(start, length)
    }

    /** The totals of the run. */
    fun totals(events: List<MonarchAiRunEvent>, now: Long?): MonarchAiAgentRunTimelineTotals {
        fun sum(pick: (MonarchAiRunEvent) -> Long?): Long? =
            events.mapNotNull(pick).takeIf { it.isNotEmpty() }?.sum()
        val duration = if (events.isEmpty()) 0L else (runEnd(events, now) - runStart(events)).coerceAtLeast(0L)
        return MonarchAiAgentRunTimelineTotals(
            durationMillis = duration,
            inputTokens = sum { it.inputTokens },
            outputTokens = sum { it.outputTokens },
            costMicros = sum { it.costMicros },
        )
    }

    /** Whether "Show in conversation" is offered for an event. */
    fun opensPart(event: MonarchAiRunEvent, hasCallback: Boolean): Boolean =
        hasCallback && (event.partId != null || event.messageId != null)

    /** The colour of an event's bar. */
    fun barColor(kind: MonarchAiRunEventKind, colors: MonarchColors): Color = when (kind) {
        MonarchAiRunEventKind.Model -> colors.aiSolid
        MonarchAiRunEventKind.Tool -> colors.aiTool
        MonarchAiRunEventKind.Handoff -> colors.infoSolid
        MonarchAiRunEventKind.Error -> colors.dangerSolid
    }

    /** The colour a running bar fades into. */
    fun runningColor(colors: MonarchColors): Color = colors.aiThinking

    /** The colour of the track behind the bars. */
    fun trackColor(colors: MonarchColors): Color = colors.bgMuted
}
