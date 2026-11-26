package com.rookhq.monarch.components.scheduling.businesshourseditor

import androidx.compose.runtime.Immutable
import com.rookhq.monarch.components.scheduling.weeklyhourseditor.MonarchWeeklyHoursInterval

/**
 * The whole value of a `MonarchBusinessHoursEditor`: plain data the app saves, with the fields of the web's
 * `BusinessHoursValue`.
 *
 * @property timeZone the IANA zone the hours are in, for example "Europe/London".
 * @property hours the weekly hours, as `MonarchWeeklyHoursEditor` takes them (ISO weekday 1 to 7, minutes).
 * @property exceptions holidays and special-hours dates.
 * @property name the calendar's name, for example "Support hours".
 * @property default whether this is the workspace's default calendar.
 * @property effectiveFrom the first day the hours apply, as the epoch milliseconds of its midnight in UTC.
 * @property effectiveTo the last day the hours apply, as the epoch milliseconds of its midnight in UTC.
 */
@Immutable
public data class MonarchBusinessHoursValue(
    public val timeZone: String,
    public val hours: List<MonarchWeeklyHoursInterval> = emptyList(),
    public val exceptions: List<MonarchBusinessHoursException> = emptyList(),
    public val name: String = "",
    public val default: Boolean = false,
    public val effectiveFrom: Long? = null,
    public val effectiveTo: Long? = null,
) {
    /**
     * Checks the value as the web's `validateBusinessHours` does: weekly hours (range, order, overlap), each
     * exception (a date, a name, no date twice, special hours that open before they close), effective dates in
     * order, and optionally a name and at least one open interval. The editor shows these; the app decides
     * whether to block saving.
     *
     * @param requireName a name is required.
     * @param requireOpen at least one open interval is required, for example so an SLA clock can run.
     */
    public fun validate(requireName: Boolean = false, requireOpen: Boolean = false): MonarchBusinessHoursErrors {
        val exceptionErrors = buildMap {
            exceptions.forEachIndexed { index, exception ->
                exceptionError(index, exception)?.let { put(index, it) }
            }
        }
        val from = effectiveFrom
        val to = effectiveTo
        return MonarchBusinessHoursErrors(
            nameRequired = requireName && name.isBlank(),
            hours = MonarchWeeklyHoursInterval.validate(hours),
            noOpenHours = requireOpen && hours.none { it.end > it.start },
            exceptions = exceptionErrors,
            effectiveOrder = from != null && to != null && to < from,
        )
    }

    private fun exceptionError(
        index: Int,
        exception: MonarchBusinessHoursException,
    ): MonarchBusinessHoursExceptionError? = when {
        exception.date == null -> MonarchBusinessHoursExceptionError.Date
        exception.name.isBlank() -> MonarchBusinessHoursExceptionError.Name
        exceptions.indexOfFirst { it.date == exception.date } != index ->
            MonarchBusinessHoursExceptionError.Duplicate
        exception.intervals.any { it.end <= it.start || it.start < 0 || it.end > MINUTES_PER_DAY } ->
            MonarchBusinessHoursExceptionError.Hours
        else -> null
    }

    private companion object {
        const val MINUTES_PER_DAY = 1440
    }
}
