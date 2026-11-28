package dev.monarchui.sample.sections.scheduling

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.overlays.popover.MonarchPopover
import dev.monarchui.components.scheduling.eventchip.MonarchEventChip
import dev.monarchui.components.scheduling.eventchip.MonarchEventChipStatus
import dev.monarchui.components.scheduling.eventpopover.MonarchEventPopover
import dev.monarchui.components.scheduling.eventpopover.MonarchEventPopoverDetail
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

/** The catalogue page of EventPopover. */
object MonarchEventPopoverSection : CataloguePage {
    override val title = "EventPopover"

    private const val HOUR = 3_600_000L
    private const val FORTY_FIVE_MINUTES = 2_700_000L
    private const val DAY = 86_400_000L

    @Composable
    override fun Content() {
        var open by rememberSaveable { mutableStateOf(false) }
        val start = remember { (System.currentTimeMillis() / HOUR + 1) * HOUR }
        val today = remember { System.currentTimeMillis() / DAY * DAY }
        CatalogueCaption("Opened from an event chip")
        Box {
            MonarchEventChip(title = "Design review", time = "Next hour", onClick = { open = true })
            MonarchPopover(expanded = open, onDismissRequest = { open = false }, label = "Design review") {
                MonarchEventPopover(
                    title = "Design review",
                    subtitle = "Product calendar",
                    start = start,
                    end = start + FORTY_FIVE_MINUTES,
                    status = MonarchEventChipStatus.Tentative,
                    details = listOf(
                        MonarchEventPopoverDetail("Hosts", "Ana Ruiz, Jordan Lee"),
                        MonarchEventPopoverDetail("Location", "Room 4"),
                    ),
                ) {
                    MonarchButton(text = "Join", onClick = { open = false }, variant = MonarchButtonVariant.Primary)
                    MonarchButton(text = "Close", onClick = { open = false })
                }
            }
        }
        CatalogueCaption("An all-day event with a note, as content")
        MonarchEventPopover(
            title = "Team offsite",
            start = today,
            end = today + DAY,
            allDay = true,
            status = MonarchEventChipStatus.Cancelled,
            note = "Cancelled because of the rail strike.",
        )
    }
}
