package com.rookhq.monarch.components.intelligence.aireasoning

import android.icu.text.MeasureFormat
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiReasoningPart
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens
import java.util.Locale
import kotlin.math.roundToLong

/** Component tokens and the pure logic of MonarchAiReasoning. */
internal object MonarchAiReasoningTokens {
    /** The quarter turn of the chevron that points right when closed and down while open. */
    const val EXPANDED_TURN: Float = 90f

    /** How often the live timer ticks. */
    const val TICK_MILLIS: Long = 1_000L

    /** How long the model reasons before the timer appears in the header. */
    const val SHOW_ELAPSED_AFTER_MILLIS: Long = 3_000L

    /** Milliseconds in a second. */
    const val MILLIS_PER_SECOND: Long = 1_000L

    private const val SECONDS_PER_MINUTE = 60L

    /** The gap between the mark and the label of the still row. */
    val IconGap: Dp = MonarchTokens.Space.space2.dp

    /** The minimum height of the still row, as tall as a disclosure header. */
    val StillRowMinHeight: Dp = MonarchTokens.Space.space12.dp

    /** The colour of the header text of the still row and of the reasoning text: muted. */
    fun textColor(colors: MonarchColors): Color = colors.textMuted

    /** The colour of the assistant mark: the strong assistant colour. */
    fun markColor(colors: MonarchColors): Color = colors.aiStrong

    /** Whether the reasoning has text to show in a disclosure. */
    fun hasBody(part: MonarchAiReasoningPart): Boolean = !part.redacted && part.text.isNotEmpty()

    /**
     * A duration as whole seconds, at least one, in the narrow unit format of
     * [locale]: "12s" under a minute, otherwise "1m 5s" or "2m".
     */
    fun duration(millis: Long, locale: Locale): String {
        val total = (millis.toDouble() / MILLIS_PER_SECOND).roundToLong().coerceAtLeast(1L)
        val format = MeasureFormat.getInstance(locale, MeasureFormat.FormatWidth.NARROW)
        val minutes = total / SECONDS_PER_MINUTE
        val seconds = total % SECONDS_PER_MINUTE
        return when {
            minutes == 0L -> format.formatMeasures(Measure(seconds, MeasureUnit.SECOND))
            seconds == 0L -> format.formatMeasures(Measure(minutes, MeasureUnit.MINUTE))
            else -> format.formatMeasures(Measure(minutes, MeasureUnit.MINUTE), Measure(seconds, MeasureUnit.SECOND))
        }
    }

    /** The duration the header shows once done: the part's own, or the one this view measured. */
    fun shownDuration(part: MonarchAiReasoningPart, measured: Long?): Long? = part.durationMs ?: measured

    /**
     * Whether the disclosure opens or closes when streaming changes, or `null`
     * when it stays as it is: it opens when reasoning starts again, and closes
     * when it finishes unless the person opened or closed it meanwhile.
     */
    fun expandedAfter(wasStreaming: Boolean, streaming: Boolean, touched: Boolean): Boolean? = when {
        streaming && !wasStreaming -> true
        !streaming && wasStreaming && !touched -> false
        else -> null
    }
}
