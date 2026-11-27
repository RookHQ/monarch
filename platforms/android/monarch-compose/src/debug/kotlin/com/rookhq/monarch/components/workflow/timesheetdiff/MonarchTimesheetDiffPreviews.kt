package com.rookhq.monarch.components.workflow.timesheetdiff

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

private const val PreviewDay = 1_768_176_000_000L

@MonarchPreviews
@Composable
internal fun MonarchTimesheetDiffPreview() {
    PreviewFrame {
        MonarchTimesheetDiff(
            before = listOf(
                MonarchTimesheetDiffEntry("1", PreviewDay, 120, project = "Pulse", activity = "Design"),
                MonarchTimesheetDiffEntry("2", PreviewDay, 45, project = "Pulse", billable = false),
            ),
            after = listOf(
                MonarchTimesheetDiffEntry("1", PreviewDay, 150, project = "Atlas", activity = "Design"),
                MonarchTimesheetDiffEntry("3", PreviewDay, 30, task = "ENG-142"),
            ),
            subtitle = "since version 3",
        )
    }
}

@MonarchPreviews
@Composable
internal fun MonarchTimesheetDiffEmptyPreview() {
    PreviewFrame {
        MonarchTimesheetDiff(diff = MonarchTimesheetDiffResult(), showBillable = false)
    }
}
