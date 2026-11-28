package com.rookhq.monarch.components.scheduling.calendartoolbar

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.R
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens of MonarchCalendarToolbar, built from primitives and roles only. */
internal object MonarchCalendarToolbarTokens {
    val Gap: Dp = MonarchTokens.Space.space2.dp
    val BusySize: Dp = MonarchTokens.Space.space4.dp

    /** The colour of the title. */
    fun titleColor(colors: MonarchColors): Color = colors.textDefault

    /** The built-in name of a view. */
    @StringRes
    fun viewNameRes(view: MonarchCalendarToolbarView): Int = when (view) {
        MonarchCalendarToolbarView.Day -> R.string.monarch_calendartoolbar_view_day
        MonarchCalendarToolbarView.Week -> R.string.monarch_calendartoolbar_view_week
        MonarchCalendarToolbarView.Month -> R.string.monarch_calendartoolbar_view_month
        MonarchCalendarToolbarView.Agenda -> R.string.monarch_calendartoolbar_view_agenda
    }

    /** The built-in unit of a view. */
    @StringRes
    fun unitRes(view: MonarchCalendarToolbarView): Int = when (view) {
        MonarchCalendarToolbarView.Day -> R.string.monarch_calendartoolbar_unit_day
        MonarchCalendarToolbarView.Week -> R.string.monarch_calendartoolbar_unit_week
        MonarchCalendarToolbarView.Month -> R.string.monarch_calendartoolbar_unit_month
        MonarchCalendarToolbarView.Agenda -> R.string.monarch_calendartoolbar_unit_agenda
    }

    /** Whether the switcher is shown: it needs a callback and more than one view. */
    fun showsSwitcher(views: List<MonarchCalendarToolbarView>, canChange: Boolean): Boolean =
        canChange && views.size > 1
}
