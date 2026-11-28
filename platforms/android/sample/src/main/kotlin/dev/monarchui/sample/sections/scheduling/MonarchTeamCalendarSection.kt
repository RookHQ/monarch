@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.sample.sections.scheduling

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.scheduling.calendartoolbar.MonarchCalendarToolbar
import dev.monarchui.components.scheduling.calendartoolbar.MonarchCalendarToolbarView
import dev.monarchui.components.scheduling.teamcalendar.MonarchTeamCalendar
import dev.monarchui.components.scheduling.teamcalendar.MonarchTeamCalendarEntry
import dev.monarchui.components.scheduling.teamcalendar.MonarchTeamCalendarHoliday
import dev.monarchui.components.scheduling.teamcalendar.MonarchTeamCalendarMember
import dev.monarchui.components.scheduling.teamcalendar.MonarchTeamCalendarPortion
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sampleicons.SampleIcons
import dev.monarchui.theme.MonarchTheme
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/** The catalogue page of TeamCalendar. */
object MonarchTeamCalendarSection : CataloguePage {
    override val title = "TeamCalendar"

    private const val DAY = 86_400_000L

    /** 1 November 2026, UTC midnight. */
    private const val NOVEMBER_1 = 1_793_491_200_000L

    private val members = listOf(
        MonarchTeamCalendarMember(id = "a", name = "Alex Kim", description = "Design"),
        MonarchTeamCalendarMember(id = "b", name = "Sam Lee", description = "Engineering"),
        MonarchTeamCalendarMember(id = "c", name = "Priya Nair", description = "Engineering"),
        MonarchTeamCalendarMember(id = "d", name = "Jordan Park", description = "Support"),
    )

    @Composable
    override fun Content() {
        var offset by rememberSaveable { mutableIntStateOf(0) }
        var tapped by rememberSaveable { mutableStateOf("Nothing tapped") }
        val month = monthAfter(NOVEMBER_1, offset)
        CatalogueCaption("A month with a toolbar")
        MonarchCalendarToolbar(
            title = monthTitle(month),
            view = MonarchCalendarToolbarView.Month,
            onToday = { offset = 0 },
            onPrevious = { offset-- },
            onNext = { offset++ },
            previousIcon = { MonarchIcon(imageVector = SampleIcons.ChevronLeft, contentDescription = null) },
            nextIcon = {
                        MonarchIcon(
                    imageVector = SampleIcons.ChevronRight,
                    contentDescription = null,
                )
            },
            views = listOf(MonarchCalendarToolbarView.Month),
        )
        MonarchTeamCalendar(
            members = members,
            entries = entries(),
            modifier = Modifier.height(360.dp),
            month = month,
            holidays = listOf(MonarchTeamCalendarHoliday(date = NOVEMBER_1 + 25 * DAY, name = "Thanksgiving")),
            today = NOVEMBER_1 + 13 * DAY,
            onEntryClick = { entry, member -> tapped = "${entry.label}: ${member.name}" },
            onCellClick = { member, _ -> tapped = "Add time away for ${member.name}" },
        )
        MonarchText(text = tapped, style = MonarchTextStyle.Caption)

        CatalogueCaption("A two-week range, read only, loading and empty")
        MonarchTeamCalendar(
            members = members.take(2),
            entries = entries(),
            modifier = Modifier.height(240.dp),
            from = NOVEMBER_1 + 7 * DAY,
            to = NOVEMBER_1 + 20 * DAY,
            today = NOVEMBER_1 + 13 * DAY,
            showLegend = false,
        )
        MonarchTeamCalendar(members = members, entries = emptyList(), loading = true)
        MonarchTeamCalendar(members = emptyList(), entries = emptyList())
    }

    @Composable
    private fun entries(): List<MonarchTeamCalendarEntry> = listOf(
        MonarchTeamCalendarEntry(
            id = "1",
            memberId = "a",
            start = NOVEMBER_1 + 8 * DAY,
            end = NOVEMBER_1 + 12 * DAY,
            label = "Annual leave",
        ),
        MonarchTeamCalendarEntry(
            id = "2",
            memberId = "b",
            start = NOVEMBER_1 + 13 * DAY,
            label = "Doctor",
            color = MonarchTheme.colors.warningSolid,
            portion = MonarchTeamCalendarPortion.SecondHalf,
        ),
        MonarchTeamCalendarEntry(
            id = "3",
            memberId = "c",
            start = NOVEMBER_1 + 16 * DAY,
            end = NOVEMBER_1 + 18 * DAY,
            label = "Conference",
            color = MonarchTheme.colors.successSolid,
            pending = true,
        ),
        MonarchTeamCalendarEntry(
            id = "4",
            memberId = "d",
            start = NOVEMBER_1 + 22 * DAY,
            end = NOVEMBER_1 + 27 * DAY,
            label = "Parental leave",
        ),
    )

    private fun monthAfter(month: Long, count: Int): Long =
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = month
            add(Calendar.MONTH, count)
        }.timeInMillis

    private fun monthTitle(month: Long): String =
        android.icu.text.DateFormat.getInstanceForSkeleton("yMMMM", Locale.getDefault()).apply {
            timeZone = android.icu.util.TimeZone.getTimeZone("UTC")
        }.format(java.util.Date(month))
}
