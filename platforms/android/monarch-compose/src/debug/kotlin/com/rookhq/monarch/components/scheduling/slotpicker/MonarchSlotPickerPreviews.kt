package com.rookhq.monarch.components.scheduling.slotpicker

import androidx.compose.runtime.Composable
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.internal.preview.PreviewIcon

private const val PreviewDay = 1_793_750_400_000L
private const val HalfHour = 1_800_000L
private const val NineUtc = PreviewDay + 9 * 2 * HalfHour

@MonarchPreviews
@Composable
internal fun MonarchSlotPickerPreview() {
    PreviewFrame {
        MonarchSlotPicker(
            slots = List(6) { index ->
                MonarchSlotPickerSlot(
                    start = NineUtc + index * HalfHour,
                    seatsLeft = if (index == 4) 3 else null,
                    enabled = index != 2,
                )
            },
            selection = NineUtc + HalfHour,
            onSelectionChange = {},
            day = PreviewDay,
            onDayChange = {},
            timeZone = "UTC",
            previousMonthIcon = { MonarchIcon(PreviewIcon, contentDescription = null) },
            nextMonthIcon = { MonarchIcon(PreviewIcon, contentDescription = null) },
            today = PreviewDay,
            is24Hour = false,
            businessTimeZone = "America/New_York",
        )
    }
}
