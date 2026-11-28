package dev.monarchui.sample.sections.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.layout.availabilitytoggle.MonarchAvailabilityToggle
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import kotlinx.coroutines.delay

/** The catalogue page of AvailabilityToggle. */
object MonarchAvailabilityToggleSection : CataloguePage {
    override val title = "AvailabilityToggle"

    private const val SAVE_MILLIS = 800L

    @Composable
    override fun Content() {
        var available by rememberSaveable { mutableStateOf(true) }
        CatalogueCaption("With a hint")
        MonarchAvailabilityToggle(available = available, onAvailableChange = { available = it }, showHint = true)

        var saved by rememberSaveable { mutableStateOf(false) }
        var requested by rememberSaveable { mutableStateOf(false) }
        var pending by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(pending) {
            if (pending) {
                delay(SAVE_MILLIS)
                saved = requested
                pending = false
            }
        }
        CatalogueCaption("Saved by the app before the value changes")
        MonarchAvailabilityToggle(
            available = saved,
            onAvailableChange = {
                requested = it
                pending = true
            },
            pending = pending,
        )

        CatalogueCaption("No permission to change it")
        MonarchAvailabilityToggle(available = true, onAvailableChange = {}, enabled = false)
    }
}
