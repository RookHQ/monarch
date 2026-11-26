package com.rookhq.monarch.components.scheduling.businesshourseditor

import androidx.compose.runtime.Composable
import com.rookhq.monarch.components.scheduling.weeklyhourseditor.MonarchWeeklyHoursInterval
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

private val PreviewValue = MonarchBusinessHoursValue(
    timeZone = "Europe/London",
    hours = (1..5).map { MonarchWeeklyHoursInterval(it, 540, 1020) },
    exceptions = listOf(
        MonarchBusinessHoursException(1_798_156_800_000L, "Christmas Day"),
        MonarchBusinessHoursException(
            1_798_070_400_000L,
            "Christmas Eve",
            listOf(MonarchBusinessHoursInterval(600, 960)),
        ),
    ),
    name = "Support hours",
)

@MonarchPreviews
@Composable
internal fun MonarchBusinessHoursEditorPreview() {
    PreviewFrame {
        MonarchBusinessHoursEditor(value = PreviewValue, onValueChange = {}, showDefault = true, is24Hour = false)
    }
}

@MonarchPreviews
@Composable
internal fun MonarchBusinessHoursEditorReadOnlyPreview() {
    PreviewFrame {
        MonarchBusinessHoursEditor(value = PreviewValue, onValueChange = {}, readOnly = true, is24Hour = true)
    }
}
