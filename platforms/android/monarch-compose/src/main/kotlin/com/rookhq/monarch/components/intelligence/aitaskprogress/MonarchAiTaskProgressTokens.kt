package com.rookhq.monarch.components.intelligence.aitaskprogress

import android.text.format.DateUtils
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.R
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiPlanStep
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiPlanStepState
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.MonarchRelativeTime
import com.rookhq.monarch.tokens.MonarchTokens
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/** How the time left is written: the resource and the number it takes. */
internal sealed interface AiTaskEta {
    /** Under a minute. */
    data object UnderAMinute : AiTaskEta

    /** A whole number of minutes, at least 1. */
    data class Minutes(val minutes: Int) : AiTaskEta

    /** A whole number of hours, at least 1. */
    data class Hours(val hours: Int) : AiTaskEta
}

/** Component tokens and rules of MonarchAiTaskProgress, built from primitives and roles only. */
@Suppress("TooManyFunctions")
internal object MonarchAiTaskProgressTokens {
    private const val PERCENT = 100
    private const val MINUTES_PER_HOUR = 60

    val SectionGap: Dp = MonarchTokens.Space.space3.dp
    val ItemGap: Dp = MonarchTokens.Space.space2.dp

    /** Whether the task can still be cancelled or watched. */
    fun active(status: MonarchAiTaskStatus): Boolean =
        status == MonarchAiTaskStatus.Running || status == MonarchAiTaskStatus.Paused

    /**
     * The completed fraction: [progress] when given, else the share of
     * finished (done, failed or skipped) steps, else 1 when done; `null` when
     * unknown.
     */
    fun fraction(progress: Float?, steps: List<MonarchAiPlanStep>, status: MonarchAiTaskStatus): Float? {
        val raw = when {
            progress != null -> progress
            steps.isNotEmpty() -> steps.count { finished(it.state) }.toFloat() / steps.size
            status == MonarchAiTaskStatus.Done -> 1f
            else -> null
        }
        return raw?.takeIf { !it.isNaN() }?.coerceIn(0f, 1f)
    }

    /** What the bar shows: the fraction, else indeterminate (`null`) while running, else empty. */
    fun barValue(fraction: Float?, status: MonarchAiTaskStatus): Float? =
        fraction ?: if (status == MonarchAiTaskStatus.Running) null else 0f

    /** The whole percentage of a fraction. */
    fun percent(fraction: Float): Int = (fraction * PERCENT).roundToInt()

    /** The tone of the status badge: the AI tone while running, as on the web. */
    fun tone(status: MonarchAiTaskStatus): MonarchTone = when (status) {
        MonarchAiTaskStatus.Running -> MonarchTone.Ai
        MonarchAiTaskStatus.Paused -> MonarchTone.Warning
        MonarchAiTaskStatus.Done -> MonarchTone.Success
        MonarchAiTaskStatus.Failed -> MonarchTone.Danger
        MonarchAiTaskStatus.Cancelled -> MonarchTone.Neutral
    }

    /** The tone of the bar: the accent while running, then the status, as on the web. */
    fun barTone(status: MonarchAiTaskStatus): MonarchTone = when (status) {
        MonarchAiTaskStatus.Running -> MonarchTone.Accent
        MonarchAiTaskStatus.Paused -> MonarchTone.Warning
        MonarchAiTaskStatus.Done -> MonarchTone.Success
        MonarchAiTaskStatus.Failed -> MonarchTone.Danger
        MonarchAiTaskStatus.Cancelled -> MonarchTone.Neutral
    }

    /** The built-in name of a status. */
    fun statusRes(status: MonarchAiTaskStatus): Int = when (status) {
        MonarchAiTaskStatus.Running -> R.string.monarch_aitaskprogress_status_running
        MonarchAiTaskStatus.Paused -> R.string.monarch_aitaskprogress_status_paused
        MonarchAiTaskStatus.Done -> R.string.monarch_aitaskprogress_status_done
        MonarchAiTaskStatus.Failed -> R.string.monarch_aitaskprogress_status_failed
        MonarchAiTaskStatus.Cancelled -> R.string.monarch_aitaskprogress_status_cancelled
    }

    /** The time left, rounded as on the web: under a minute, whole minutes, then whole hours. */
    fun eta(millis: Long): AiTaskEta {
        if (millis < DateUtils.MINUTE_IN_MILLIS) return AiTaskEta.UnderAMinute
        val minutes = maxOf(1L, (millis.toDouble() / DateUtils.MINUTE_IN_MILLIS).roundToLong()).toInt()
        return if (minutes < MINUTES_PER_HOUR) {
            AiTaskEta.Minutes(minutes)
        } else {
            AiTaskEta.Hours(maxOf(1, (minutes.toDouble() / MINUTES_PER_HOUR).roundToInt()))
        }
    }

    /** Whether the time left is shown. */
    fun showsEta(status: MonarchAiTaskStatus, etaMillis: Long?): Boolean =
        status == MonarchAiTaskStatus.Running && etaMillis != null && etaMillis >= 0

    /** "5 minutes ago" from [startedAt] to [now], in the default locale, by the shared Monarch rule (the web rule). */
    fun relative(startedAt: Long, now: Long, locale: Locale = Locale.getDefault()): String =
        MonarchRelativeTime.format(startedAt, now, locale)

    /** Whether a status change is announced, and whether assertively. `null` for no announcement. */
    fun announcement(previous: MonarchAiTaskStatus?, current: MonarchAiTaskStatus): Boolean? = when {
        previous == null || previous == current -> null
        else -> current == MonarchAiTaskStatus.Failed
    }

    private fun finished(state: MonarchAiPlanStepState): Boolean =
        state == MonarchAiPlanStepState.Done ||
            state == MonarchAiPlanStepState.Failed ||
            state == MonarchAiPlanStepState.Skipped
}
