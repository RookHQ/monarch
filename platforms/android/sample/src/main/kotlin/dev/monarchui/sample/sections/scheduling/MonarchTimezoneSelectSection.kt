package dev.monarchui.sample.sections.scheduling

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.scheduling.timezoneselect.MonarchTimezoneSelect
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

private const val RecentCount = 3

/** The catalogue page of TimezoneSelect. */
object MonarchTimezoneSelectSection : CataloguePage {
    override val title = "TimezoneSelect"

    @Composable
    override fun Content() {
        var zone by rememberSaveable { mutableStateOf<String?>("Europe/London") }
        var recent by rememberSaveable { mutableStateOf(listOf("Asia/Tokyo")) }
        var meeting by rememberSaveable { mutableStateOf<String?>(null) }
        CatalogueCaption("Every zone, with detected, pinned and recent zones")
        MonarchTimezoneSelect(
            label = "Time zone",
            selected = zone,
            onSelectedChange = { next ->
                zone = next
                if (next != null) recent = (listOf(next) + recent).distinct().take(RecentCount)
            },
            helperText = "Search by city, country or offset, such as +5:30.",
            pinned = listOf("Europe/London", "America/New_York"),
            recent = recent,
        )
        CatalogueCaption("A short list, required")
        MonarchTimezoneSelect(
            label = "Meeting time zone",
            selected = meeting,
            onSelectedChange = { meeting = it },
            placeholder = "Choose a time zone",
            errorText = if (meeting == null) "Choose a time zone." else null,
            zones = listOf("UTC", "Europe/Paris", "Asia/Kolkata", "America/Los_Angeles"),
            showDetected = false,
        )
    }
}
