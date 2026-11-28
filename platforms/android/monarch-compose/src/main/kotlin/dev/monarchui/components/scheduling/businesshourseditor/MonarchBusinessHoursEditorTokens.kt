package dev.monarchui.components.scheduling.businesshourseditor

import android.icu.text.DateFormat
import android.icu.util.TimeZone
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens
import java.util.Date
import java.util.Locale

/** Component tokens and the pure logic of MonarchBusinessHoursEditor. */
internal object MonarchBusinessHoursEditorTokens {
    /** The gap between the sections of the editor. */
    val SectionGap: Dp = MonarchTokens.Space.space6.dp

    /** The gap between the fields of a section and between the rows of the exception list. */
    val FieldGap: Dp = MonarchTokens.Space.space3.dp

    /** The gap between the controls of one row. */
    val ControlGap: Dp = MonarchTokens.Space.space2.dp

    /** The opening and closing time a date switched to special hours starts with: 10:00 to 16:00, as on the web. */
    val SpecialHoursDefault: MonarchBusinessHoursInterval = MonarchBusinessHoursInterval(start = 600, end = 960)

    /** The colour of an error message. */
    fun errorColor(colors: MonarchColors): Color = colors.dangerStrong

    /** The colour of help text, the empty note and the read-only details. */
    fun noteColor(colors: MonarchColors): Color = colors.textMuted

    /** [value] with the exception at [index] replaced by [exception]. */
    fun withException(
        value: MonarchBusinessHoursValue,
        index: Int,
        exception: MonarchBusinessHoursException,
    ): MonarchBusinessHoursValue =
        value.copy(exceptions = value.exceptions.mapIndexed { i, e -> if (i == index) exception else e })

    /** [value] without the exception at [index]. */
    fun withoutException(value: MonarchBusinessHoursValue, index: Int): MonarchBusinessHoursValue =
        value.copy(exceptions = value.exceptions.filterIndexed { i, _ -> i != index })

    /** [value] with a new, empty exception at the end, as the web's Add date. */
    fun withNewException(value: MonarchBusinessHoursValue): MonarchBusinessHoursValue =
        value.copy(exceptions = value.exceptions + MonarchBusinessHoursException(date = null, name = ""))

    /** [exception] closed all day, or open at its first interval (or the default interval) when [special]. */
    fun withSpecialHours(exception: MonarchBusinessHoursException, special: Boolean): MonarchBusinessHoursException =
        exception.copy(
            intervals = if (special) listOf(exception.intervals.firstOrNull() ?: SpecialHoursDefault) else emptyList(),
        )

    /**
     * Keeps one stable id per exception row, so a row's touched state follows it when an earlier row is removed:
     * removes the id of [removed] if given, then adds or drops ids at the end to match [count].
     */
    fun reconcile(ids: List<Int>, count: Int, next: Int, removed: Int? = null): List<Int> {
        val kept = if (removed != null && removed in ids.indices) ids.filterIndexed { i, _ -> i != removed } else ids
        return if (kept.size >= count) kept.take(count) else kept + (next until next + count - kept.size)
    }

    /** "Jan 1, 2027" for the UTC midnight of a date, in [locale]. */
    fun date(millis: Long, locale: Locale): String {
        val format = DateFormat.getInstanceForSkeleton(DATE, locale)
        format.timeZone = TimeZone.getTimeZone(UTC)
        return format.format(Date(millis))
    }

    private const val DATE = "yMMdd"
    private const val UTC = "UTC"
}
