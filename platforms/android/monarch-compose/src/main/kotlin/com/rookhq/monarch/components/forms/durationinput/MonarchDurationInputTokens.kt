package com.rookhq.monarch.components.forms.durationinput

import android.icu.text.MeasureFormat
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import com.rookhq.monarch.theme.MonarchColors
import java.util.Locale

/** Why a duration text was not saved. */
internal enum class DurationInputProblem {
    /** The text cannot be read. */
    Invalid,

    /** The text reads, but the duration is outside the bounds. */
    OutOfRange,
}

/** What reading the text does: save a value (`null` when empty), or keep the text and show a problem. */
internal sealed interface DurationInputCommit {
    /** Save [minutes]; `null` clears the value. */
    data class Save(val minutes: Int?) : DurationInputCommit

    /** Keep the text and show [problem]. */
    data class Reject(val problem: DurationInputProblem) : DurationInputCommit
}

/** Component tokens and the pure rules of MonarchDurationInput. */
internal object MonarchDurationInputTokens {
    /** Minutes added or removed by Page Up and Page Down on a hardware keyboard. */
    const val PAGE_STEP: Int = DurationInputMath.MINUTES_PER_HOUR

    /** The default step: a quarter of an hour. */
    const val DEFAULT_STEP: Int = 15

    /** Reads the text: empty saves `null`, a duration inside the bounds saves it, anything else is rejected. */
    fun commit(parsed: DurationInputParse, minimum: Int, maximum: Int?): DurationInputCommit = when (parsed) {
        DurationInputParse.Empty -> DurationInputCommit.Save(null)
        DurationInputParse.Invalid -> DurationInputCommit.Reject(DurationInputProblem.Invalid)
        is DurationInputParse.Minutes -> if (DurationInputMath.outOfRange(parsed.minutes, minimum, maximum)) {
            DurationInputCommit.Reject(DurationInputProblem.OutOfRange)
        } else {
            DurationInputCommit.Save(parsed.minutes)
        }
    }

    /** Reads [text] with the app's [parse] (given trimmed, non-blank text) or the built-in reader. */
    fun read(text: String, parse: ((String) -> Int?)?, minutesPerDay: Int): DurationInputParse = when {
        text.isBlank() -> DurationInputParse.Empty
        parse != null -> parse(text.trim())?.let { DurationInputParse.Minutes(it) } ?: DurationInputParse.Invalid
        else -> DurationInputMath.parse(text, minutesPerDay)
    }

    /** The value a step starts from: the text if it reads, else the saved value, else the minimum. */
    fun stepBase(parsed: DurationInputParse, saved: Int?, minimum: Int): Int =
        (parsed as? DurationInputParse.Minutes)?.minutes ?: saved ?: minimum

    /** Whether a step up changes the value. */
    fun canIncrease(value: Int?, maximum: Int?): Boolean = value == null || maximum == null || value < maximum

    /** Whether a step down changes the value. */
    fun canDecrease(value: Int?, minimum: Int): Boolean = value == null || value > minimum

    /** The error to show: the app's first, then the field's own. Blank strings count as none. */
    fun shownError(error: String?, problem: String?): String? =
        error?.takeIf { it.isNotBlank() } ?: problem?.takeIf { it.isNotBlank() }

    /** The spoken duration in the locale's words: "1 hour, 30 minutes", "45 minutes", "2 hours". */
    fun valueText(minutes: Int, locale: Locale): String {
        val total = maxOf(0, minutes)
        val hours = total / DurationInputMath.MINUTES_PER_HOUR
        val rest = total % DurationInputMath.MINUTES_PER_HOUR
        val format = MeasureFormat.getInstance(locale, MeasureFormat.FormatWidth.WIDE)
        val hourMeasure = Measure(hours, MeasureUnit.HOUR)
        val minuteMeasure = Measure(rest, MeasureUnit.MINUTE)
        return when {
            hours == 0 -> format.formatMeasures(minuteMeasure)
            rest == 0 -> format.formatMeasures(hourMeasure)
            else -> format.formatMeasures(hourMeasure, minuteMeasure)
        }
    }

    /** The direction and size of the step a hardware key takes: arrows by [step], Page Up and Down by an hour. */
    fun keyDelta(key: Key, step: Int): Pair<Int, Int>? = when (key) {
        Key.DirectionUp -> 1 to step
        Key.DirectionDown -> -1 to step
        Key.PageUp -> 1 to PAGE_STEP
        Key.PageDown -> -1 to PAGE_STEP
        else -> null
    }

    /** The colour of the − and + signs: muted like every icon in a field, disabled at the bounds. */
    fun stepper(enabled: Boolean, colors: MonarchColors): Color =
        if (enabled) colors.textMuted else colors.textDisabled
}
