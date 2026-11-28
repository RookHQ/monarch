package dev.monarchui.sample.sections.scheduling

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.scheduling.slotpicker.MonarchSlotPicker
import dev.monarchui.components.scheduling.slotpicker.MonarchSlotPickerSlot
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sampleicons.SampleIcons

private const val DayMillis = 86_400_000L
private const val HalfHour = 1_800_000L
private const val Nov2 = 1_793_577_600_000L
private const val NineHours = 32_400_000L
private const val SlotCount = 6
private const val TakenSlot = 2
private const val GroupSlot = 4
private const val GroupSeats = 3
private val Bookable = listOf(2L, 3L, 4L, 7L, 8L, 9L).map { Nov2 + it * DayMillis }.toSet()

/** Sample opening times: from 09:00 UTC every 30 minutes, with a taken time and a small group. */
private fun sampleSlots(day: Long): List<MonarchSlotPickerSlot> = List(SlotCount) { index ->
    MonarchSlotPickerSlot(
        start = day + NineHours + index * HalfHour,
        seatsLeft = if (index == GroupSlot) GroupSeats else null,
        enabled = index != TakenSlot,
    )
}

/** The catalogue page of SlotPicker. */
object MonarchSlotPickerSection : CataloguePage {
    override val title = "SlotPicker"

    @Composable
    override fun Content() {
        var day by rememberSaveable { mutableStateOf<Long?>(Nov2 + 2 * DayMillis) }
        var selection by rememberSaveable { mutableStateOf<Long?>(null) }
        var zone by rememberSaveable { mutableStateOf("Europe/London") }
        CatalogueCaption("Pick a day, then a time; change the zone to see the times move")
        MonarchSlotPicker(
            slots = day?.let(::sampleSlots).orEmpty(),
            selection = selection,
            onSelectionChange = { selection = it },
            day = day,
            onDayChange = {
                day = it
                selection = null
            },
            timeZone = zone,
            previousMonthIcon = { MonarchIcon(SampleIcons.ChevronLeft, contentDescription = null) },
            nextMonthIcon = { MonarchIcon(SampleIcons.ChevronRight, contentDescription = null) },
            availableDays = Bookable,
            minimumDate = Nov2,
            today = Nov2,
            onTimeZoneChange = { zone = it },
            pinnedTimeZones = listOf("Europe/London", "America/New_York", "Asia/Kolkata"),
            businessTimeZone = "Europe/London",
        )
        MonarchText(text = "Selected: ${selection ?: "none"}")
    }
}
