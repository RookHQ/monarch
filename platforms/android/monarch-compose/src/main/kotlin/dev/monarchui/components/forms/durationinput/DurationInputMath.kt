package dev.monarchui.components.forms.durationinput

import kotlin.math.abs
import kotlin.math.roundToLong

/** What a duration text reads as. */
internal sealed interface DurationInputParse {
    /** The text is empty or only spaces. */
    data object Empty : DurationInputParse

    /** A duration in whole minutes. */
    data class Minutes(val minutes: Int) : DurationInputParse

    /** Text that cannot be read as a duration. */
    data object Invalid : DurationInputParse
}

/** Parsing, formatting and stepping of MonarchDurationInput, the same rules as the web and iOS. */
internal object DurationInputMath {
    /** Minutes in an hour. */
    const val MINUTES_PER_HOUR: Int = 60

    /** Minutes in a calendar day, the default length of a "d". */
    const val MINUTES_PER_DAY: Int = 1440

    private enum class Unit { Days, Hours, Minutes }

    /** Unit words, in English, as the web component reads them. */
    private val unitWords: Map<String, Unit> = mapOf(
        "d" to Unit.Days, "day" to Unit.Days, "days" to Unit.Days,
        "h" to Unit.Hours, "hr" to Unit.Hours, "hrs" to Unit.Hours, "hour" to Unit.Hours, "hours" to Unit.Hours,
        "m" to Unit.Minutes, "min" to Unit.Minutes, "mins" to Unit.Minutes, "minute" to Unit.Minutes,
        "minutes" to Unit.Minutes,
    )

    private val bare = Regex("""^\d+(\.\d+)?$""")
    private val clock = Regex("""^(\d+):(\d{2})$""")
    private val part = Regex("""(\d+(?:\.\d+)?)\s*([a-z]+)\s*""")

    /**
     * Reads a duration as whole minutes. Accepts `90`, `90m`, `1h 30m`,
     * `1h30`, `1.5h`, `1,5h`, `1:30`, `2 hours 15 minutes` and `1d 4h`.
     */
    fun parse(text: String, minutesPerDay: Int = MINUTES_PER_DAY): DurationInputParse {
        val source = text.trim().lowercase().replace(',', '.')
        val time = clock.matchEntire(source)
        return when {
            source.isEmpty() -> DurationInputParse.Empty
            // A bare number is minutes.
            bare.matches(source) -> DurationInputParse.Minutes(rounded(source.toDouble()))
            // A clock: hours and two-digit minutes below 60.
            time != null -> parseClock(time.groupValues[1].toInt(), time.groupValues[2].toInt())
            else -> parseUnits(source, minutesPerDay)
        }
    }

    private fun parseClock(hours: Int, minutes: Int): DurationInputParse = if (minutes < MINUTES_PER_HOUR) {
        DurationInputParse.Minutes(hours * MINUTES_PER_HOUR + minutes)
    } else {
        DurationInputParse.Invalid
    }

    private fun parseUnits(source: String, minutesPerDay: Int): DurationInputParse {
        var total = 0.0
        var index = 0
        var lastUnit: Unit? = null
        while (index < source.length) {
            val match = part.find(source, index)?.takeIf { it.range.first == index } ?: break
            val unit = unitWords[match.groupValues[2]] ?: return DurationInputParse.Invalid
            val number = match.groupValues[1].toDouble()
            total += number * when (unit) {
                Unit.Days -> minutesPerDay
                Unit.Hours -> MINUTES_PER_HOUR
                Unit.Minutes -> 1
            }
            index = match.range.last + 1
            lastUnit = unit
        }
        // "1h30": a bare number after hours is minutes.
        val rest = source.substring(index).trim()
        val restValid = rest.isEmpty() || (lastUnit == Unit.Hours && rest.all { it in '0'..'9' })
        return if (lastUnit == null || !restValid) {
            DurationInputParse.Invalid
        } else {
            DurationInputParse.Minutes(rounded(total + (rest.toDoubleOrNull() ?: 0.0)))
        }
    }

    /** Writes minutes as `0m`, `45m`, `1h 30m`, `26h`, or `1d 2h` when days are the largest unit. */
    fun format(
        minutes: Int,
        largestUnit: MonarchDurationInputUnit = MonarchDurationInputUnit.Hours,
        minutesPerDay: Int = MINUTES_PER_DAY,
    ): String {
        val total = maxOf(0, minutes)
        if (total == 0) return "0m"
        val days = if (largestUnit == MonarchDurationInputUnit.Days && minutesPerDay > 0) total / minutesPerDay else 0
        val afterDays = total - days * minutesPerDay
        val hours = afterDays / MINUTES_PER_HOUR
        val rest = afterDays % MINUTES_PER_HOUR
        return listOfNotNull(
            "${days}d".takeIf { days > 0 },
            "${hours}h".takeIf { hours > 0 },
            "${rest}m".takeIf { rest > 0 },
        ).joinToString(" ")
    }

    /** Moves [base] one step up or down, snapped to the step grid (50 + 15 is 60, 50 − 15 is 45), within bounds. */
    fun stepped(base: Int, step: Int, direction: Int, minimum: Int, maximum: Int?): Int {
        val size = maxOf(1, abs(step))
        val next = if (direction > 0) {
            Math.floorDiv(base, size) * size + size
        } else {
            -Math.floorDiv(-base, size) * size - size
        }
        return clamped(next, minimum, maximum)
    }

    /** [value] within the bounds; a maximum below the minimum counts as the minimum. */
    fun clamped(value: Int, minimum: Int, maximum: Int?): Int {
        val low = maxOf(minimum, value)
        return if (maximum != null) minOf(low, maxOf(minimum, maximum)) else low
    }

    /** Whether [value] is outside the bounds. */
    fun outOfRange(value: Int, minimum: Int, maximum: Int?): Boolean =
        value < minimum || (maximum != null && value > maximum)

    private fun rounded(value: Double): Int =
        value.roundToLong().coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt()
}
