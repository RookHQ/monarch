package dev.monarchui.components.scheduling.eventpopover

import androidx.compose.runtime.Composable
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.scheduling.eventchip.MonarchEventChipStatus
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import java.util.TimeZone

private const val PreviewStart = 1_763_130_600_000L
private const val FortyFiveMinutes = 2_700_000L
private const val AllDayStart = 1_763_078_400_000L
private const val TwoDays = 172_800_000L

@MonarchPreviews
@Composable
internal fun MonarchEventPopoverPreview() {
    PreviewFrame {
        MonarchEventPopover(
            title = "Design review",
            subtitle = "Product calendar",
            start = PreviewStart,
            end = PreviewStart + FortyFiveMinutes,
            status = MonarchEventChipStatus.Tentative,
            details = listOf(
                MonarchEventPopoverDetail("Hosts", "Ana, Jordan"),
                MonarchEventPopoverDetail("Location", "Room 4"),
            ),
            timeZone = "America/New_York",
        ) {
            MonarchButton(text = "Join", onClick = {}, variant = MonarchButtonVariant.Primary)
            MonarchButton(text = "Edit", onClick = {})
        }
    }
}

@MonarchPreviews
@Composable
internal fun MonarchEventPopoverAllDayPreview() {
    PreviewFrame {
        MonarchEventPopover(
            title = "Offsite",
            start = AllDayStart,
            end = AllDayStart + TwoDays,
            allDay = true,
            status = MonarchEventChipStatus.Cancelled,
            note = "Cancelled because of the rail strike.",
        )
    }
}
