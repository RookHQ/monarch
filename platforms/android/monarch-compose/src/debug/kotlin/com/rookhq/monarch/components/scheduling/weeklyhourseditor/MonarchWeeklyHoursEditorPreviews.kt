package com.rookhq.monarch.components.scheduling.weeklyhourseditor

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

private val PreviewHours = listOf(
    MonarchWeeklyHoursInterval(1, 540, 750),
    MonarchWeeklyHoursInterval(1, 810, 1050),
    MonarchWeeklyHoursInterval(2, 540, 1020),
    MonarchWeeklyHoursInterval(3, 600, 540),
)

@MonarchPreviews
@Composable
internal fun MonarchWeeklyHoursEditorPreview() {
    PreviewFrame {
        MonarchWeeklyHoursEditor(
            value = PreviewHours,
            onValueChange = {},
            is24Hour = false,
        )
    }
}

@MonarchPreviews
@Composable
internal fun MonarchWeeklyHoursEditorReadOnlyPreview() {
    PreviewFrame {
        MonarchWeeklyHoursEditor(value = PreviewHours, onValueChange = {}, is24Hour = true, readOnly = true)
    }
}
