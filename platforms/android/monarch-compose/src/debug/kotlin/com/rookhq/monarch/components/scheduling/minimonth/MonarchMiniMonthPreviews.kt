package com.rookhq.monarch.components.scheduling.minimonth

import androidx.compose.runtime.Composable
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.internal.scheduling.MonarchCalendarDates
import com.rookhq.monarch.sampleicons.SampleIcons

/** 14 November 2026, UTC midnight. */
private const val PreviewDay = 1_794_614_400_000L

/** 5 November 2026, UTC midnight. */
private const val PreviewToday = 1_793_836_800_000L

@MonarchPreviews
@Composable
internal fun MonarchMiniMonthPreview() {
    PreviewFrame {
        MonarchMiniMonth(
            selection = PreviewDay,
            onSelectionChange = {},
            previousMonthIcon = {
                MonarchIcon(SampleIcons.ChevronLeft, null)
            },
            nextMonthIcon = {
                MonarchIcon(SampleIcons.ChevronRight, null)
            },
            today = PreviewToday,
            showWeekNumbers = true,
            dayInfo = { day ->
                if (MonarchCalendarDates.dayOfMonth(day) % 4 == 0) {
                    MonarchMiniMonthDayInfo(listOf(null, MonarchTone.Success), "2 events")
                } else {
                    null
                }
            },
        )
    }
}
