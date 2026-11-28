package dev.monarchui.sample.sections.collaboration

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.collaboration.recordingindicator.MonarchRecordingIndicator
import dev.monarchui.components.collaboration.recordingindicator.MonarchRecordingIndicatorVariant
import dev.monarchui.components.forms.switchrow.MonarchSwitchRow
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

/** The catalogue page of RecordingIndicator. */
object MonarchRecordingIndicatorSection : CataloguePage {
    override val title = "RecordingIndicator"

    @Composable
    override fun Content() {
        var recording by rememberSaveable { mutableStateOf(true) }
        var paused by rememberSaveable { mutableStateOf(false) }
        var since by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
        CatalogueCaption("Live")
        MonarchRecordingIndicator(
            recording = recording,
            paused = paused,
            since = since,
            onStop = { recording = false },
        )
        if (!recording) {
            MonarchButton(text = "Start recording", onClick = {
                since = System.currentTimeMillis()
                paused = false
                recording = true
            })
        }
        MonarchSwitchRow(checked = paused, onCheckedChange = { paused = it }, label = "Paused", enabled = recording)
        MonarchRecordingIndicatorVariant.entries.forEach { variant ->
            CatalogueCaption(variant.name)
            MonarchRecordingIndicator(recording = true, variant = variant, elapsedMillis = 83_000L)
            MonarchRecordingIndicator(recording = true, variant = variant, paused = true, elapsedMillis = 83_000L)
        }
    }
}
