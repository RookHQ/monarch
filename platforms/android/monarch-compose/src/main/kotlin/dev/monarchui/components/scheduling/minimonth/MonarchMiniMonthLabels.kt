package dev.monarchui.components.scheduling.minimonth

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchMiniMonth`. Every
 * `null` keeps the built-in string `monarch_minimonth_<key>`.
 *
 * @property previousMonth the name of the previous-month button, "Previous month".
 * @property nextMonth the name of the next-month button, "Next month".
 * @property today said before the date of today, "Today".
 * @property week the name of the week-number column, "Week".
 * @property weekNumber what TalkBack reads for a week number: "Week 46".
 */
@Immutable
public class MonarchMiniMonthLabels(
    public val previousMonth: String? = null,
    public val nextMonth: String? = null,
    public val today: String? = null,
    public val week: String? = null,
    public val weekNumber: ((Int) -> String)? = null,
)
