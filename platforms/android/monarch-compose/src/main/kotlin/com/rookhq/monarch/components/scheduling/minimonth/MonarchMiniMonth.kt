package com.rookhq.monarch.components.scheduling.minimonth

import android.icu.text.DateFormatSymbols
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.max
import com.rookhq.monarch.R
import com.rookhq.monarch.accessibility.monarchFocusRing
import com.rookhq.monarch.components.actions.iconbutton.MonarchIconButton
import com.rookhq.monarch.internal.MonarchDefaultIcons
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.internal.scheduling.MonarchCalendarDates
import com.rookhq.monarch.theme.MonarchTheme
import java.util.Calendar
import java.util.Locale

/**
 * A month calendar for choosing a day, with an earliest and latest day,
 * days the app marks unavailable, ISO week numbers and up to three dots
 * under days that have events.
 *
 * Built by Monarch: the Material 3 `DatePicker` cannot show event dots or
 * week numbers. Days are epoch milliseconds of their midnight in UTC, as in
 * the Material 3 `DatePickerState`. Every day is a 48dp target reserved in
 * the layout; when the width offered is less than seven of them (336dp, more
 * with week numbers) the grid scrolls sideways instead of shrinking the days.
 *
 * TalkBack reads each day as its full date, "Today" for today, and the
 * description of its dots; the selected day is selected, unavailable days
 * are disabled, and the month title is a heading that announces itself when
 * the month changes.
 *
 * @param selection the selected day, or `null` for none.
 * @param onSelectionChange called with the day the user chooses: the UTC midnight of that date.
 * @param previousMonthIcon the icon of the previous-month button, for example a `MonarchIcon` of a chevron that
 * points to the start, with a `null` content description. Monarch bundles no icons; use an icon that mirrors
 * in right-to-left layouts.
 * @param nextMonthIcon the icon of the next-month button.
 * @param modifier the modifier of the calendar.
 * @param visibleMonth any day of the month on screen. `null` lets the calendar keep it, starting on the month of
 * [selection] or of today.
 * @param onVisibleMonthChange called with the first day of the month the user moves to.
 * @param minimumDate the earliest day that can be chosen. Earlier months cannot be reached.
 * @param maximumDate the latest day that can be chosen. Later months cannot be reached. A latest day before the
 * earliest day leaves only the earliest day.
 * @param today today's date, for example in the workspace's time zone. `null` is today on the device.
 * @param firstDayOfWeek the first column, `Calendar.SUNDAY` to `Calendar.SATURDAY`. `null` follows the locale.
 * @param showWeekNumbers shows ISO 8601 week numbers in a column at the start.
 * @param showOutsideDays shows the neighbouring months' days, dimmed and not selectable.
 * @param fixedWeeks always six week rows, so the height does not change between months.
 * @param enabled whether days and the month buttons can be used.
 * @param isDateDisabled returns `true` for a day that cannot be chosen. Called for every day of the month shown.
 * @param dayInfo the dots and their description for a day. Called for every day of the month shown.
 * @param labels replacements for the built-in strings.
 */
@Suppress("LongParameterList", "LongMethod")
@Composable
public fun MonarchMiniMonth(
    selection: Long?,
    onSelectionChange: (Long) -> Unit,
    previousMonthIcon: @Composable () -> Unit,
    nextMonthIcon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    visibleMonth: Long? = null,
    onVisibleMonthChange: ((Long) -> Unit)? = null,
    minimumDate: Long? = null,
    maximumDate: Long? = null,
    today: Long? = null,
    firstDayOfWeek: Int? = null,
    showWeekNumbers: Boolean = false,
    showOutsideDays: Boolean = true,
    fixedWeeks: Boolean = true,
    enabled: Boolean = true,
    isDateDisabled: ((Long) -> Boolean)? = null,
    dayInfo: ((Long) -> MonarchMiniMonthDayInfo?)? = null,
    labels: MonarchMiniMonthLabels = MonarchMiniMonthLabels(),
) {
    val locale = LocalConfiguration.current.locales[0]
    val todayDay = today?.let(MonarchCalendarDates::dayOf) ?: remember { MonarchCalendarDates.today() }
    var ownMonth by rememberSaveable { mutableLongStateOf(MonarchCalendarDates.monthOf(selection ?: todayDay)) }
    val month = MonarchMiniMonthTokens.clampMonth(visibleMonth ?: ownMonth, minimumDate, maximumDate)
    val latest = MonarchMiniMonthTokens.latest(minimumDate, maximumDate)
    val canGoBack = minimumDate == null ||
        MonarchCalendarDates.monthIndex(month) > MonarchCalendarDates.monthIndex(minimumDate)
    val canGoForward = latest == null ||
        MonarchCalendarDates.monthIndex(month) < MonarchCalendarDates.monthIndex(latest)
    val go = { count: Int ->
        val next = MonarchCalendarDates.addMonths(month, count)
        ownMonth = next
        onVisibleMonthChange?.invoke(next)
    }
    val formats = remember(locale) { MiniMonthFormats(locale) }
    val weekStart = firstDayOfWeek ?: remember(locale) { Calendar.getInstance(locale).firstDayOfWeek }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(MonarchMiniMonthTokens.HeaderGap)) {
        MonthHeader(
            title = formats.month(month),
            previous = MonthButton(
                monarchString(labels.previousMonth, R.string.monarch_minimonth_previous_month),
                enabled && canGoBack,
                { go(-1) },
                previousMonthIcon,
            ),
            next = MonthButton(
                monarchString(labels.nextMonth, R.string.monarch_minimonth_next_month),
                enabled && canGoForward,
                { go(1) },
                nextMonthIcon,
            ),
        )
        val grid = MiniMonthGrid(
            month = month,
            weeks = MonarchCalendarDates.weeks(month, weekStart, fixedWeeks),
            selection = selection,
            today = todayDay,
            minimumDate = minimumDate,
            maximumDate = maximumDate,
            enabled = enabled,
            showWeekNumbers = showWeekNumbers,
            showOutsideDays = showOutsideDays,
            isDateDisabled = isDateDisabled,
            dayInfo = dayInfo,
            formats = formats,
            todayLabel = monarchString(labels.today, R.string.monarch_minimonth_today),
            weekLabel = monarchString(labels.week, R.string.monarch_minimonth_week),
            weekNumberLabel = labels.weekNumber,
            onSelect = onSelectionChange,
        )
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val weekColumn = if (showWeekNumbers) MonarchMiniMonthTokens.WeekColumnWidth else Dp(0f)
            val offered = if (constraints.hasBoundedWidth) maxWidth else MonarchMiniMonthTokens.CellSize * DAYS
            val cell = max(MonarchMiniMonthTokens.CellSize, (offered - weekColumn) / DAYS)
            val scrolls = weekColumn + cell * DAYS > offered
            Box(modifier = if (scrolls) Modifier.horizontalScroll(rememberScrollState()) else Modifier) {
                grid.Content(cell, weekColumn)
            }
        }
    }
}

private const val DAYS = MonarchCalendarDates.DAYS_PER_WEEK

/** One of the two month buttons. */
private class MonthButton(
    val label: String,
    val enabled: Boolean,
    val onClick: () -> Unit,
    val icon: @Composable () -> Unit,
)

@Composable
private fun MonthHeader(title: String, previous: MonthButton, next: MonthButton) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        HeaderButton(previous)
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(MonarchMiniMonthTokens.TitleGap, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                modifier = Modifier.semantics {
                    heading()
                    liveRegion = LiveRegionMode.Polite
                },
                style = MonarchTheme.typography.bodyStrong,
                color = MonarchTheme.colors.textDefault,
            )
            Box(Modifier.clearAndSetSemantics { }) { MonarchDefaultIcons.ChevronDown() }
        }
        HeaderButton(next)
    }
}

@Composable
private fun HeaderButton(button: MonthButton) {
    MonarchIconButton(
        label = button.label,
        onClick = button.onClick,
        enabled = button.enabled,
        icon = button.icon,
    )
}

/** Names of months, weekdays and days in the locale, in UTC. */
private class MiniMonthFormats(locale: Locale) {
    private val utc = android.icu.util.TimeZone.getTimeZone("UTC")
    private val monthFormat = android.icu.text.DateFormat.getInstanceForSkeleton("yMMMM", locale).apply {
        timeZone = utc
    }
    private val dayFormat = android.icu.text.DateFormat.getInstanceForSkeleton("yMMMMEEEEd", locale).apply {
        timeZone = utc
    }
    private val numbers = android.icu.text.NumberFormat.getIntegerInstance(locale).apply { isGroupingUsed = false }
    private val symbols = DateFormatSymbols(locale)
    val narrowWeekdays: Array<String> = symbols.getWeekdays(DateFormatSymbols.STANDALONE, DateFormatSymbols.ABBREVIATED)
    val weekdays: Array<String> = symbols.getWeekdays(DateFormatSymbols.STANDALONE, DateFormatSymbols.WIDE)

    fun month(month: Long): String = monthFormat.format(java.util.Date(month))

    fun day(day: Long): String = dayFormat.format(java.util.Date(day))

    fun number(value: Int): String = numbers.format(value.toLong())
}

/** The weekday header and the weeks of one month. */
@Suppress("LongParameterList")
private class MiniMonthGrid(
    val month: Long,
    val weeks: List<List<Long>>,
    val selection: Long?,
    val today: Long,
    val minimumDate: Long?,
    val maximumDate: Long?,
    val enabled: Boolean,
    val showWeekNumbers: Boolean,
    val showOutsideDays: Boolean,
    val isDateDisabled: ((Long) -> Boolean)?,
    val dayInfo: ((Long) -> MonarchMiniMonthDayInfo?)?,
    val formats: MiniMonthFormats,
    val todayLabel: String,
    val weekLabel: String,
    val weekNumberLabel: ((Int) -> String)?,
    val onSelect: (Long) -> Unit,
) {
    @Composable
    fun Content(cell: Dp, weekColumn: Dp) {
        Column {
            WeekdayRow(cell, weekColumn)
            weeks.forEach { week -> WeekRow(week, cell, weekColumn) }
        }
    }

    @Composable
    private fun WeekdayRow(cell: Dp, weekColumn: Dp) {
        Row {
            if (showWeekNumbers) {
                Box(modifier = Modifier.width(weekColumn).semantics { contentDescription = weekLabel })
            }
            weeks.first().forEach { day ->
                val weekday = MonarchCalendarDates.dayOfWeek(day)
                Box(
                    modifier = Modifier
                        .width(cell)
                        .semantics { contentDescription = formats.weekdays[weekday] },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = formats.narrowWeekdays[weekday],
                        style = MonarchTheme.typography.caption,
                        color = MonarchTheme.colors.textSubtle,
                    )
                }
            }
        }
    }

    @Composable
    private fun WeekRow(week: List<Long>, cell: Dp, weekColumn: Dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showWeekNumbers) {
                val number = MonarchCalendarDates.isoWeek(week)
                val spoken = weekNumberLabel?.invoke(number)
                    ?: stringResource(R.string.monarch_minimonth_week_number, number)
                Box(
                    modifier = Modifier
                        .width(weekColumn)
                        .clearAndSetSemantics { contentDescription = spoken },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = formats.number(number),
                        style = MonarchTheme.typography.caption,
                        color = MonarchTheme.colors.textSubtle,
                    )
                }
            }
            week.forEach { day -> DayCell(day, cell) }
        }
    }

    @Composable
    private fun DayCell(day: Long, cell: Dp) {
        val tokens = MonarchMiniMonthTokens
        val inMonth = MonarchCalendarDates.monthIndex(day) == MonarchCalendarDates.monthIndex(month)
        if (!inMonth && !showOutsideDays) {
            Spacer(modifier = Modifier.size(cell, tokens.CellSize))
            return
        }
        val state = tokens.dayState(
            day = day,
            month = month,
            selection = selection,
            today = today,
            minimum = minimumDate,
            maximum = maximumDate,
            enabled = enabled,
            disabledByApp = inMonth && isDateDisabled?.invoke(day) == true,
        )
        val info = if (inMonth) dayInfo?.invoke(day) else null
        val colors = tokens.dayColors(state, MonarchTheme.colors)
        val shape = MonarchShapeDefaults.Mark
        val name = listOfNotNull(todayLabel.takeIf { state.today }, formats.day(day), info?.description)
            .filter { it.isNotBlank() }
            .joinToString(", ")
        val interaction = if (inMonth) {
            Modifier
                .monarchFocusRing(shape)
                .selectable(
                    selected = state.selected,
                    enabled = state.available,
                    role = Role.Button,
                    onClick = { onSelect(day) },
                )
                // TalkBack reads the full date in place of the number.
                .semantics { contentDescription = name }
        } else {
            // A day of a neighbouring month is context only: not selectable and not read.
            Modifier.clearAndSetSemantics {}
        }
        val ring = colors.border?.let { Modifier.border(MonarchShapeDefaults.BorderWidth, it, shape) } ?: Modifier
        Box(
            modifier = Modifier
                .width(cell)
                .heightIn(min = tokens.CellSize)
                .then(interaction),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .sizeIn(minWidth = tokens.MarkSize, minHeight = tokens.MarkSize)
                        .background(colors.container, shape)
                        .then(ring),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = formats.number(MonarchCalendarDates.dayOfMonth(day)),
                        style = MonarchTheme.typography.label,
                        fontWeight = if (state.today) FontWeight(tokens.TODAY_WEIGHT) else null,
                        color = colors.content,
                        textDecoration = if (state.disabledByApp) TextDecoration.LineThrough else null,
                    )
                }
                Dots(info)
            }
        }
    }

    @Composable
    private fun Dots(info: MonarchMiniMonthDayInfo?) {
        val tokens = MonarchMiniMonthTokens
        val theme = MonarchTheme.colors
        Row(
            modifier = Modifier.padding(top = tokens.DotGap).heightIn(min = tokens.DotSize),
            horizontalArrangement = Arrangement.spacedBy(tokens.DotGap),
        ) {
            info?.dots?.take(MonarchMiniMonthDayInfo.MAX_DOTS)?.forEach { tone ->
                val color = tokens.dotColor(tone, theme)
                Box(modifier = Modifier.size(tokens.DotSize).drawBehind { drawCircle(color) })
            }
        }
    }
}
