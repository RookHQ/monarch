package dev.monarchui.components.scheduling.teamcalendar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.R
import dev.monarchui.components.feedback.banner.MonarchBanner
import dev.monarchui.components.feedback.banner.MonarchBannerAnnouncement
import dev.monarchui.components.feedback.emptystate.MonarchEmptyState
import dev.monarchui.components.feedback.skeleton.MonarchSkeleton
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.monarchString
import dev.monarchui.internal.scheduling.MonarchCalendarDates
import java.util.Calendar

/**
 * Who is away when: people down the start, the days of a month or a custom range across, absences as bars (half
 * days as half bars, pending ones outlined), weekends and holidays shaded and today ringed.
 *
 * Built by Monarch: Material 3 has no grid of people by days. It is a Compose `LazyColumn` of rows with the day
 * header pinned by `stickyHeader`; the people column stays put while the days of every row scroll sideways in
 * one shared `ScrollState`, starting at the day before today when today is shown. Days are 48dp wide and rows at
 * least 48dp tall, so every target is reserved in the layout. Give it a bounded height.
 *
 * TalkBack reads the corner as the grid's name, each person as a heading, each day header as its full date with
 * "Today" and the holiday's name, and each bar as one element: label, person, days, half day and "pending". A
 * long press on a bar or a holiday shows its label in a Material 3 tooltip. Data in, taps out: put a
 * `MonarchCalendarToolbar` above it to move between months.
 *
 * @param members the rows, in display order.
 * @param entries the entries; days outside the range are left out, and an entry for an unknown member is ignored.
 * @param modifier the modifier of the calendar.
 * @param month any day of the month shown, as epoch milliseconds read in UTC. `null` is the month of [today].
 * Ignored when [from] and [to] are both set.
 * @param from the first day of a custom range.
 * @param to the last day of a custom range, inclusive. At most 366 days are shown.
 * @param holidays non-working days for everyone, shaded and named.
 * @param weekendDays the shaded days of the week, `Calendar.SUNDAY` to `Calendar.SATURDAY`.
 * @param today today's date, for example in the workspace's time zone. `null` is today on the device.
 * @param loading shows a placeholder instead of the grid.
 * @param error shows this message in a danger banner instead of the grid.
 * @param showLegend shows the line under the grid that explains shading, half bars and faded bars.
 * @param labels replacements for the built-in strings.
 * @param onEntryClick called with the entry and its member when a bar is tapped. Without it bars are read but not
 * buttons.
 * @param onCellClick called with the member and the day (UTC midnight) when an empty day is tapped, for example to
 * add time away. Without it empty days take no taps.
 * @param empty replaces the built-in empty state shown when there are no members.
 */
@Suppress("LongParameterList", "LongMethod")
@ExperimentalMonarchApi
@Composable
public fun MonarchTeamCalendar(
    members: List<MonarchTeamCalendarMember>,
    entries: List<MonarchTeamCalendarEntry>,
    modifier: Modifier = Modifier,
    month: Long? = null,
    from: Long? = null,
    to: Long? = null,
    holidays: List<MonarchTeamCalendarHoliday> = emptyList(),
    weekendDays: Set<Int> = setOf(Calendar.SATURDAY, Calendar.SUNDAY),
    today: Long? = null,
    loading: Boolean = false,
    error: String? = null,
    showLegend: Boolean = true,
    labels: MonarchTeamCalendarLabels = MonarchTeamCalendarLabels(),
    onEntryClick: ((MonarchTeamCalendarEntry, MonarchTeamCalendarMember) -> Unit)? = null,
    onCellClick: ((MonarchTeamCalendarMember, Long) -> Unit)? = null,
    empty: (@Composable () -> Unit)? = null,
) {
    val tokens = MonarchTeamCalendarTokens
    when {
        loading -> {
            val loadingLabel = monarchString(labels.loading, R.string.monarch_teamcalendar_loading)
            Box(modifier = modifier.semantics { contentDescription = loadingLabel }) {
                MonarchSkeleton(modifier = Modifier.fillMaxWidth().height(tokens.PlaceholderHeight))
            }
            return
        }
        error != null -> {
            MonarchBanner(
                message = error,
                modifier = modifier,
                tone = MonarchTone.Danger,
                announcement = MonarchBannerAnnouncement.Polite,
            )
            return
        }
        members.isEmpty() -> {
            if (empty != null) {
                Box(modifier = modifier) { empty() }
            } else {
                MonarchEmptyState(
                    title = monarchString(labels.emptyTitle, R.string.monarch_teamcalendar_empty_title),
                    modifier = modifier,
                    message = monarchString(labels.emptyDescription, R.string.monarch_teamcalendar_empty_description),
                )
            }
            return
        }
    }

    val locale = LocalConfiguration.current.locales[0]
    val todayDay = today?.let(MonarchCalendarDates::dayOf) ?: remember { MonarchCalendarDates.today() }
    val (first, last) = tokens.range(month, from, to, todayDay)
    val custom = from != null && to != null
    val days = remember(first, last) { tokens.days(first, last) }
    val formats = remember(locale) { TeamCalendarFormats(locale) }
    val holidayNames = remember(holidays) { holidays.associate { MonarchCalendarDates.dayOf(it.date) to it.name } }
    val cells = remember(entries, days) {
        entries.groupBy { it.memberId }.mapValues { (_, own) -> tokens.cells(own, days) }
    }
    val shaded = remember(days, weekendDays, holidayNames) { days.map { tokens.shaded(it, weekendDays, holidayNames) } }
    val rangeName = if (custom) formats.range(first, last) else formats.month(first)
    val holidayFormat = stringResource(R.string.monarch_teamcalendar_holiday)
    val rangeFormat = stringResource(R.string.monarch_teamcalendar_range)
    val strings = TeamCalendarStrings(
        person = monarchString(labels.person, R.string.monarch_teamcalendar_person),
        grid = labels.grid?.invoke(rangeName) ?: stringResource(R.string.monarch_teamcalendar_grid, rangeName),
        today = monarchString(labels.today, R.string.monarch_teamcalendar_today),
        holiday = labels.holiday ?: { name -> String.format(locale, holidayFormat, name) },
        range = labels.range ?: { start, end -> String.format(locale, rangeFormat, start, end) },
        firstHalf = monarchString(labels.firstHalf, R.string.monarch_teamcalendar_first_half),
        secondHalf = monarchString(labels.secondHalf, R.string.monarch_teamcalendar_second_half),
        pending = monarchString(labels.pending, R.string.monarch_teamcalendar_pending),
        legend = if (showLegend) monarchString(labels.legend, R.string.monarch_teamcalendar_legend) else null,
    )

    val scroll = rememberScrollState()
    val todayIndex = days.indexOf(todayDay)
    val density = LocalDensity.current
    LaunchedEffect(first, last, todayIndex) {
        // Start at the day before today, so today is not hidden under the edge; otherwise at the first day.
        val target = if (todayIndex > 0) with(density) { (tokens.CellSize * (todayIndex - 1)).roundToPx() } else 0
        scroll.scrollTo(target)
    }

    TeamCalendarGrid(
        model = TeamCalendarModel(
            members = members,
            days = days,
            cells = cells,
            shaded = shaded,
            holidays = holidayNames,
            today = todayDay,
            formats = formats,
            strings = strings,
            onEntryClick = onEntryClick,
            onCellClick = onCellClick,
        ),
        scroll = scroll,
        modifier = modifier,
    )
}
