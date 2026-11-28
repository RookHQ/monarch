package com.rookhq.monarch.components.scheduling.businesshourseditor

import androidx.compose.runtime.Immutable

/**
 * One open interval of a date that replaces the weekly hours in a [MonarchBusinessHoursException].
 *
 * @property start the opening time, in minutes from midnight.
 * @property end the closing time, in minutes from midnight, exclusive and at most 1440 (the end of the day).
 */
@Immutable
public data class MonarchBusinessHoursInterval(
    public val start: Int,
    public val end: Int,
)
