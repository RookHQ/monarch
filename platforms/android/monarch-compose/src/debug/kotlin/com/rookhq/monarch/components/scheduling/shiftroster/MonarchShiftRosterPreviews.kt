@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.components.scheduling.shiftroster

import androidx.compose.runtime.Composable
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import java.util.TimeZone

/** Monday 2 November 2026, 00:00 UTC. */
private const val PreviewMonday = 1_793_577_600_000L
private const val PreviewDay = 86_400_000L
private val PreviewUtc: TimeZone = TimeZone.getTimeZone("UTC")

private val PreviewShifts = listOf(
    MonarchShiftRosterShift("m", "Morning", start = 6 * 60, end = 14 * 60, shortName = "M"),
    MonarchShiftRosterShift("n", "Night", start = 22 * 60, end = 6 * 60, shortName = "N"),
)

private val PreviewMembers = listOf(
    MonarchShiftRosterMember("a", "Alex Kim", description = "Support"),
    MonarchShiftRosterMember("b", "Sam Lee", description = "Operations"),
    MonarchShiftRosterMember("c", "Priya Nair"),
)

private val PreviewValue = mapOf(
    "a" to mapOf(
        PreviewMonday to MonarchShiftRosterAssignment.Shift("m"),
        PreviewMonday + PreviewDay to MonarchShiftRosterAssignment.Off,
    ),
    "b" to mapOf(PreviewMonday to MonarchShiftRosterAssignment.Shift("n")),
    "c" to mapOf(PreviewMonday to MonarchShiftRosterAssignment.Off),
)

@Composable
private fun PreviewRoster(
    layout: MonarchShiftRosterLayout,
    days: Int = 7,
    readOnly: Boolean = false,
) {
    MonarchShiftRoster(
        members = PreviewMembers,
        shifts = PreviewShifts,
        from = PreviewMonday,
        value = PreviewValue,
        onValueChange = { _, _ -> },
        days = days,
        layout = layout,
        readOnly = readOnly,
        today = PreviewMonday,
        timeZone = PreviewUtc.id,
        is24Hour = false,
    )
}

@MonarchPreviews
@Composable
internal fun MonarchShiftRosterByDayPreview() {
    PreviewFrame { PreviewRoster(MonarchShiftRosterLayout.ByDay) }
}

@MonarchPreviews
@Composable
internal fun MonarchShiftRosterGridPreview() {
    PreviewFrame { PreviewRoster(MonarchShiftRosterLayout.Grid, days = 3) }
}

@MonarchPreviews
@Composable
internal fun MonarchShiftRosterByPersonReadOnlyPreview() {
    PreviewFrame { PreviewRoster(MonarchShiftRosterLayout.ByPerson, days = 3, readOnly = true) }
}

@MonarchPreviews
@Composable
internal fun MonarchShiftRosterStatesPreview() {
    PreviewFrame {
        MonarchShiftRoster(
            members = PreviewMembers,
            shifts = PreviewShifts,
            from = PreviewMonday,
            value = emptyMap(),
            onValueChange = { _, _ -> },
            loading = true,
        )
        MonarchShiftRoster(
            members = PreviewMembers,
            shifts = PreviewShifts,
            from = PreviewMonday,
            value = emptyMap(),
            onValueChange = { _, _ -> },
            error = "The roster could not be loaded.",
        )
        MonarchShiftRoster(
            members = emptyList(),
            shifts = PreviewShifts,
            from = PreviewMonday,
            value = emptyMap(),
            onValueChange = { _, _ -> },
        )
    }
}
