package dev.monarchui.components.scheduling.minimonth

import androidx.compose.runtime.Composable
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.internal.scheduling.MonarchCalendarDates
import dev.monarchui.sampleicons.SampleIcons

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
