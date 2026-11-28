package dev.monarchui.components.intelligence.aitableanswer

import dev.monarchui.components.intelligence.aiconversation.MonarchAiJSON
import java.text.Collator
import java.text.NumberFormat
import java.util.Locale

/** How MonarchAiTableAnswer reads, compares and writes the values of the model's rows. Every function is pure. */
internal object MonarchAiTableAnswerValues {
    private val NumberPattern = Regex("^[-+−]?(\\d{1,3}(,\\d{3})+|\\d+)?(\\.\\d+)?%?$")
    private val DatePattern = Regex(
        "^(\\d{4})-(\\d{2})-(\\d{2})(?:[T ](\\d{2}):(\\d{2})(?::(\\d{2})(?:\\.(\\d+))?)?(Z|[+-]\\d{2}:?\\d{2})?)?$",
    )
    private val Digits = Regex("\\d+|\\D+")
    private const val MAX_FRACTION_DIGITS = 6
    private const val WHOLE_LIMIT = 1e15
    private const val MILLIS_PER_SECOND = 1000L
    private const val SECONDS_PER_MINUTE = 60L
    private const val MINUTES_PER_HOUR = 60L
    private const val MILLIS_DIGITS = 3

    /** Whether a value is missing: absent, null or blank text. */
    fun isBlank(value: MonarchAiJSON?): Boolean = when (value) {
        null, MonarchAiJSON.Null -> true
        is MonarchAiJSON.Text -> value.value.isBlank()
        else -> false
    }

    /** The value as a number: a JSON number, or text such as "1,200", "-3.5", "−3" or "12%". */
    fun number(value: MonarchAiJSON?): Double? = when (value) {
        is MonarchAiJSON.Number -> value.value.takeIf { it.isFinite() }
        is MonarchAiJSON.Text -> {
            val text = value.value.trim()
            if (text.none { it.isDigit() } || !NumberPattern.matches(text)) {
                null
            } else {
                text.replace(",", "").replace('−', '-').removeSuffix("%").removePrefix("+")
                    .toDoubleOrNull()?.takeIf { it.isFinite() }
            }
        }
        else -> null
    }

    /**
     * The value as a time in epoch milliseconds: ISO 8601 text such as
     * "2024-10-14" or "2024-10-14T09:30:00Z". A date or time without a zone is read as UTC.
     */
    @Suppress("MagicNumber", "ComplexCondition")
    fun date(value: MonarchAiJSON?): Long? {
        val text = (value as? MonarchAiJSON.Text)?.value?.trim() ?: return null
        val g = DatePattern.matchEntire(text)?.groupValues ?: return null
        val month = g[2].toInt()
        val day = g[3].toInt()
        val hour = g[4].ifEmpty { "0" }.toInt()
        val minute = g[5].ifEmpty { "0" }.toInt()
        val second = g[6].ifEmpty { "0" }.toInt()
        val valid = month in 1..12 && day in 1..31 && hour <= 23 && minute <= 59 && second <= 59
        val millis = g[7].take(MILLIS_DIGITS).padEnd(MILLIS_DIGITS, '0').toInt()
        val seconds = ((daysFromCivil(g[1].toInt(), month, day) * 24 + hour) * MINUTES_PER_HOUR + minute) *
            SECONDS_PER_MINUTE + second - offsetSeconds(g[8])
        return if (valid) seconds * MILLIS_PER_SECOND + millis else null
    }

    /** The zone offset of `Z`, `+02:00` or `-0530`, in seconds. */
    private fun offsetSeconds(zone: String): Long {
        if (zone.isEmpty() || zone == "Z") return 0
        val digits = zone.drop(1).replace(":", "")
        val minutes = digits.take(2).toLong() * MINUTES_PER_HOUR + digits.drop(2).toLong()
        return (if (zone.startsWith("-")) -minutes else minutes) * SECONDS_PER_MINUTE
    }

    /** Days since 1970-01-01 of a proleptic Gregorian date (Howard Hinnant's algorithm). */
    @Suppress("MagicNumber")
    private fun daysFromCivil(year: Int, month: Int, day: Int): Long {
        val y = (if (month <= 2) year - 1 else year).toLong()
        val era = (if (y >= 0) y else y - 399) / 400
        val yearOfEra = y - era * 400
        val dayOfYear = (153 * (month + (if (month > 2) -3 else 9)) + 2) / 5 + day - 1
        val dayOfEra = yearOfEra * 365 + yearOfEra / 4 - yearOfEra / 100 + dayOfYear
        return era * 146_097 + dayOfEra - 719_468
    }

    /** The type of a column from its values, ignoring blanks: number, then date, otherwise text. */
    fun inferType(values: List<MonarchAiJSON?>): MonarchAiTableColumnType {
        val present = values.filterNot(::isBlank)
        return when {
            present.isEmpty() -> MonarchAiTableColumnType.Text
            present.all { number(it) != null } -> MonarchAiTableColumnType.Number
            present.all { date(it) != null } -> MonarchAiTableColumnType.Date
            else -> MonarchAiTableColumnType.Text
        }
    }

    /** The text of a value: text as is, whole numbers without a decimal point, arrays and objects as JSON. */
    fun text(value: MonarchAiJSON?): String = when {
        value == null || isBlank(value) -> ""
        value is MonarchAiJSON.Text -> value.value
        value is MonarchAiJSON.Bool -> value.value.toString()
        value is MonarchAiJSON.Number -> {
            val number = value.value
            if (number == Math.rint(number) && kotlin.math.abs(number) < WHOLE_LIMIT) {
                number.toLong().toString()
            } else {
                number.toString()
            }
        }
        else -> value.toJsonString()
    }

    /** The text shown in a cell: JSON numbers in a number column are formatted for [locale]; the rest is [text]. */
    fun display(value: MonarchAiJSON?, type: MonarchAiTableColumnType, locale: Locale): String =
        if (value is MonarchAiJSON.Number && type == MonarchAiTableColumnType.Number && value.value.isFinite()) {
            NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = MAX_FRACTION_DIGITS }
                .format(value.value)
        } else {
            text(value)
        }

    /** Compares text ignoring case and accents, with runs of digits compared as numbers: "Item 2" before "item 10". */
    fun compareText(a: String, b: String, locale: Locale): Int {
        val collator = Collator.getInstance(locale).apply { strength = Collator.PRIMARY }
        val left = Digits.findAll(a).map { it.value }.toList()
        val right = Digits.findAll(b).map { it.value }.toList()
        for (index in 0 until minOf(left.size, right.size)) {
            val x = left[index]
            val y = right[index]
            val result = if (x[0].isDigit() && y[0].isDigit()) {
                x.trimStart('0').length.compareTo(y.trimStart('0').length)
                    .takeIf { it != 0 } ?: x.trimStart('0').compareTo(y.trimStart('0'))
            } else {
                collator.compare(x, y)
            }
            if (result != 0) return result
        }
        return left.size.compareTo(right.size)
    }
}
