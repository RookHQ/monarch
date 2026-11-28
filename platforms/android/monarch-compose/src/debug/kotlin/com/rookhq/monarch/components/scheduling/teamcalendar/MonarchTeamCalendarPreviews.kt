@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.components.scheduling.teamcalendar

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.theme.MonarchTheme
import com.rookhq.monarch.tokens.MonarchTokens

private const val PreviewDay = 86_400_000L

/** 1 November 2026, UTC midnight. */
private const val PreviewNovember1 = 1_793_491_200_000L

private const val PreviewConferenceLastDay = PreviewNovember1 + 5 * PreviewDay

private val PreviewMembers = listOf(
    MonarchTeamCalendarMember(id = "a", name = "Alex Kim", description = "Design"),
    MonarchTeamCalendarMember(id = "b", name = "Sam Lee", description = "Engineering"),
    MonarchTeamCalendarMember(id = "c", name = "Priya Nair"),
)

@Composable
private fun previewEntries(): List<MonarchTeamCalendarEntry> = listOf(
    MonarchTeamCalendarEntry(
        id = "1",
        memberId = "a",
        start = PreviewNovember1 + PreviewDay,
        end = PreviewNovember1 + 4 * PreviewDay,
        label = "Annual leave",
    ),
    MonarchTeamCalendarEntry(
        id = "2",
        memberId = "b",
        start = PreviewNovember1 + 2 * PreviewDay,
        label = "Doctor",
        portion = MonarchTeamCalendarPortion.FirstHalf,
        color = MonarchTheme.colors.warningSolid,
    ),
    MonarchTeamCalendarEntry(
        id = "3",
        memberId = "c",
        start = PreviewNovember1 + 3 * PreviewDay,
        end = PreviewNovember1 + 5 * PreviewDay,
        label = "Conference",
        pending = true,
        portions = mapOf(PreviewConferenceLastDay to MonarchTeamCalendarPortion.FirstHalf),
    ),
)

@MonarchPreviews
@Composable
internal fun MonarchTeamCalendarPreview() {
    PreviewFrame {
        MonarchTeamCalendar(
            members = PreviewMembers,
            entries = previewEntries(),
            modifier = Modifier.height((MonarchTokens.Space.space24 * 3).dp),
            from = PreviewNovember1,
            to = PreviewNovember1 + 13 * PreviewDay,
            holidays = listOf(MonarchTeamCalendarHoliday(PreviewNovember1 + 10 * PreviewDay, "Veterans Day")),
            today = PreviewNovember1 + 2 * PreviewDay,
            onEntryClick = { _, _ -> },
            onCellClick = { _, _ -> },
        )
    }
}

@MonarchPreviews
@Composable
internal fun MonarchTeamCalendarStatesPreview() {
    PreviewFrame {
        MonarchTeamCalendar(members = emptyList(), entries = emptyList())
        MonarchTeamCalendar(members = PreviewMembers, entries = emptyList(), error = "The calendar could not load.")
        MonarchTeamCalendar(members = PreviewMembers, entries = emptyList(), loading = true)
    }
}
