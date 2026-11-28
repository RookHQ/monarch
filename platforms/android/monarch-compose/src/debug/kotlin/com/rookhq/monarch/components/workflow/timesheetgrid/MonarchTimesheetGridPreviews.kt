@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.components.workflow.timesheetgrid

import androidx.compose.runtime.Composable
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

/** Monday 12 January 2026, 00:00 UTC. */
private const val PreviewMonday = 1_768_176_000_000L
private const val PreviewDay = 86_400_000L

private val PreviewDays = listOf(
    MonarchTimesheetGridDay(PreviewMonday, lockReason = "Approved", expectedMinutes = 480),
    MonarchTimesheetGridDay(PreviewMonday + PreviewDay, today = true, expectedMinutes = 480),
    MonarchTimesheetGridDay(PreviewMonday + 2 * PreviewDay, nonWorking = true, note = "Holiday"),
)

private val PreviewRows = listOf(
    MonarchTimesheetGridRow(
        id = "atlas",
        label = "Atlas",
        description = "Billable",
        cells = mapOf(PreviewMonday to 240, PreviewMonday + PreviewDay to 90),
    ),
    MonarchTimesheetGridRow(
        id = "timer",
        label = "Timer entries",
        cells = mapOf(PreviewMonday + PreviewDay to 75),
        readOnly = true,
    ),
)

@MonarchPreviews
@Composable
internal fun MonarchTimesheetGridDayPreview() {
    PreviewFrame {
        MonarchTimesheetGrid(
            days = PreviewDays,
            rows = PreviewRows,
            layout = MonarchTimesheetGridLayout.Day,
            onCellChange = { _, _, _ -> },
        )
    }
}

@MonarchPreviews
@Composable
internal fun MonarchTimesheetGridTablePreview() {
    PreviewFrame {
        MonarchTimesheetGrid(
            days = PreviewDays,
            rows = PreviewRows,
            layout = MonarchTimesheetGridLayout.Grid,
            summaryRows = listOf(
                MonarchTimesheetGridSummaryRow("in", "Attendance", mapOf(PreviewMonday to "7h 55m"), total = "7h 55m"),
            ),
            onCellChange = { _, _, _ -> },
        )
    }
}

@MonarchPreviews
@Composable
internal fun MonarchTimesheetGridReadOnlyPreview() {
    PreviewFrame {
        MonarchTimesheetGrid(
            days = PreviewDays,
            rows = PreviewRows,
            layout = MonarchTimesheetGridLayout.Grid,
            readOnly = true,
            maximumMinutesPerDay = 300,
            onCellActivate = { _, _ -> },
        )
    }
}
