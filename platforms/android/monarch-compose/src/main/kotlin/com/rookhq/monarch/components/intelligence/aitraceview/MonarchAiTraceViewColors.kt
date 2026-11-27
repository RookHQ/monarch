package com.rookhq.monarch.components.intelligence.aitraceview

import androidx.compose.ui.graphics.Color
import com.rookhq.monarch.theme.MonarchColors

/** The colours of MonarchAiTraceView, by role. Every function is pure. */
internal object MonarchAiTraceViewColors {
    /** The colour of a span's bar: by status when it did not finish well, else by kind. */
    fun barColor(span: MonarchAiTraceSpan, colors: MonarchColors): Color = when (span.status) {
        MonarchAiTraceSpanStatus.Error -> colors.dangerSolid
        MonarchAiTraceSpanStatus.Cancelled -> colors.textDisabled
        MonarchAiTraceSpanStatus.Running -> colors.aiThinking
        MonarchAiTraceSpanStatus.Ok -> when (span.kind) {
            MonarchAiTraceSpanKind.Model -> colors.aiSolid
            MonarchAiTraceSpanKind.Tool -> colors.aiTool
            MonarchAiTraceSpanKind.Retrieval -> colors.infoSolid
            MonarchAiTraceSpanKind.Agent -> colors.textDefault
            MonarchAiTraceSpanKind.Guardrail -> colors.warningStrong
            MonarchAiTraceSpanKind.Other -> colors.borderStrong
        }
    }

    /** The colour of the track behind a bar. */
    fun trackColor(colors: MonarchColors): Color = colors.bgMuted

    /** The fill of the row whose details are open. */
    fun selectedColor(colors: MonarchColors): Color = colors.accentSubtle

    /** The colour of an error message. */
    fun errorColor(colors: MonarchColors): Color = colors.dangerStrong
}
