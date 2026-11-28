package com.rookhq.monarch.components.workflow.timesheetgrid

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.rookhq.monarch.R
import com.rookhq.monarch.components.forms.durationinput.DurationInputMath
import com.rookhq.monarch.components.forms.durationinput.MonarchDurationInputTokens
import com.rookhq.monarch.internal.monarchString
import java.util.Date
import java.util.Locale

/** The resolved strings and formats of a timesheet grid. */
@Suppress("LongParameterList")
internal class TimesheetGridText(
    val caption: String,
    val rowHeader: String,
    val total: String,
    val dayTotal: String,
    val days: String,
    val rowActions: (String) -> String,
    val actionsText: String,
    val cellLabel: (String, String) -> String,
    val locked: String,
    val noTime: String,
    val invalid: String,
    val tooLong: (String) -> String,
    val saveFailed: String,
    val expected: (String) -> String,
    val overLimit: String,
    val empty: String,
    val today: String,
    val saving: String,
    val periodTotal: (String) -> String,
    val duration: (Int) -> String,
    val spokenDuration: (Int) -> String,
    val shortDay: (Long) -> String,
    val longDay: (Long) -> String,
)

/** Resolves [labels] against the built-in strings and the formats of the configuration's locale. */
@Composable
internal fun rememberTimesheetGridText(labels: MonarchTimesheetGridLabels): TimesheetGridText {
    val locale = LocalConfiguration.current.locales[0]
    val dates = remember(locale) { TimesheetGridDates(locale) }
    val rowActions = stringResource(R.string.monarch_timesheetgrid_row_actions)
    val cellLabel = stringResource(R.string.monarch_timesheetgrid_cell_label)
    val tooLong = stringResource(R.string.monarch_timesheetgrid_too_long)
    val expected = stringResource(R.string.monarch_timesheetgrid_expected)
    val periodTotal = stringResource(R.string.monarch_timesheetgrid_period_total)
    return TimesheetGridText(
        caption = monarchString(labels.caption, R.string.monarch_timesheetgrid_caption),
        rowHeader = monarchString(labels.rowHeader, R.string.monarch_timesheetgrid_row_header),
        total = monarchString(labels.total, R.string.monarch_timesheetgrid_total),
        dayTotal = monarchString(labels.dayTotal, R.string.monarch_timesheetgrid_day_total),
        days = monarchString(labels.days, R.string.monarch_timesheetgrid_days),
        rowActions = labels.rowActions ?: { rowActions.format(it) },
        actionsText = monarchString(labels.actionsText, R.string.monarch_timesheetgrid_actions_text),
        cellLabel = labels.cellLabel ?: { row, day -> cellLabel.format(row, day) },
        locked = monarchString(labels.locked, R.string.monarch_timesheetgrid_locked),
        noTime = monarchString(labels.noTime, R.string.monarch_timesheetgrid_no_time),
        invalid = monarchString(labels.invalid, R.string.monarch_timesheetgrid_invalid),
        tooLong = labels.tooLong ?: { tooLong.format(it) },
        saveFailed = monarchString(labels.saveFailed, R.string.monarch_timesheetgrid_save_failed),
        expected = labels.expected ?: { expected.format(it) },
        overLimit = monarchString(labels.overLimit, R.string.monarch_timesheetgrid_over_limit),
        empty = monarchString(labels.empty, R.string.monarch_timesheetgrid_empty),
        today = monarchString(labels.today, R.string.monarch_timesheetgrid_today),
        saving = monarchString(labels.saving, R.string.monarch_timesheetgrid_saving),
        periodTotal = labels.periodTotal ?: { periodTotal.format(it) },
        duration = labels.duration ?: { DurationInputMath.format(it) },
        spokenDuration = labels.spokenDuration ?: { MonarchDurationInputTokens.valueText(it, locale) },
        shortDay = labels.shortDay ?: dates::short,
        longDay = labels.longDay ?: dates::long,
    )
}

/** The headings of days, "Mon 12" and "Mon, Jan 12", read in UTC as the Material 3 date picker writes them. */
internal class TimesheetGridDates(locale: Locale) {
    // English writes the weekday first, "Mon 12", in every region the web example does.
    private val shortFormat = if (locale.language == "en") {
        java.text.SimpleDateFormat(ENGLISH_SHORT, locale).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
    } else {
        format(SHORT_SKELETON, locale)
    }
    private val longFormat = format(LONG_SKELETON, locale)

    fun short(day: Long): String = shortFormat.format(Date(day))

    fun long(day: Long): String = longFormat.format(Date(day))

    private companion object {
        const val SHORT_SKELETON = "EEEd"
        const val ENGLISH_SHORT = "EEE d"
        const val LONG_SKELETON = "EEEMMMd"

        fun format(skeleton: String, locale: Locale): android.icu.text.DateFormat =
            android.icu.text.DateFormat.getInstanceForSkeleton(skeleton, locale).apply {
                timeZone = android.icu.util.TimeZone.getTimeZone("UTC")
            }
    }
}
