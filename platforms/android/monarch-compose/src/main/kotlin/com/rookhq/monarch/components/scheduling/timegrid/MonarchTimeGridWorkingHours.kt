package com.rookhq.monarch.components.scheduling.timegrid

import androidx.compose.runtime.Immutable

/**
 * Working or opening hours on one weekday, for a `MonarchTimeGrid`. Time outside them is shaded. Give several
 * intervals for a day with a break.
 *
 * ```
 * // Monday to Friday, 09:00 to 17:00.
 * (1..5).map { MonarchTimeGridWorkingHours(weekday = it, start = 9 * 60, end = 17 * 60) }
 * ```
 *
 * @property weekday the ISO weekday: 1 is Monday, 7 is Sunday, as on the web and iOS.
 * @property start wall-clock minutes from midnight when the hours start.
 * @property end wall-clock minutes from midnight when the hours end (exclusive).
 */
@Immutable
public class MonarchTimeGridWorkingHours(
    public val weekday: Int,
    public val start: Int,
    public val end: Int,
)
