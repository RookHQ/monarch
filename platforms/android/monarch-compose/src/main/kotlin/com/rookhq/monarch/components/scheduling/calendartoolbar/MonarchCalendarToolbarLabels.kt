package com.rookhq.monarch.components.scheduling.calendartoolbar

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchCalendarToolbar`.
 * Every `null` keeps the built-in string `monarch_calendartoolbar_<key>`.
 *
 * @property today the Today button, "Today".
 * @property previous the name of the previous arrow for a unit: "Previous week".
 * @property next the name of the next arrow for a unit: "Next week".
 * @property view the name of the view switcher, "Calendar view".
 * @property viewName the name of a view in the switcher: "Week".
 * @property unit the unit a view moves by, used in the arrow names: "week".
 * @property loading the name of the busy indicator, "Loading".
 */
@Immutable
public class MonarchCalendarToolbarLabels(
    public val today: String? = null,
    public val previous: ((String) -> String)? = null,
    public val next: ((String) -> String)? = null,
    public val view: String? = null,
    public val viewName: ((MonarchCalendarToolbarView) -> String)? = null,
    public val unit: ((MonarchCalendarToolbarView) -> String)? = null,
    public val loading: String? = null,
)
