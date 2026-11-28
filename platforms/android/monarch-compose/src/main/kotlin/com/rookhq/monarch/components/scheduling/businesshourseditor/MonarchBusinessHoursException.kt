package com.rookhq.monarch.components.scheduling.businesshourseditor

import androidx.compose.runtime.Immutable

/**
 * A date that differs from the weekly hours of a [MonarchBusinessHoursValue]: a holiday, closed all day, or a
 * date with special hours.
 *
 * @property date the date, as the epoch milliseconds of its midnight in UTC (as Material 3 `DatePickerState`
 * gives it), or `null` while the person is still choosing.
 * @property name what the date is, for example "New Year's Day".
 * @property intervals the open intervals that day. Empty: closed all day. The editor edits the first one.
 */
@Immutable
public data class MonarchBusinessHoursException(
    public val date: Long?,
    public val name: String,
    public val intervals: List<MonarchBusinessHoursInterval> = emptyList(),
)
