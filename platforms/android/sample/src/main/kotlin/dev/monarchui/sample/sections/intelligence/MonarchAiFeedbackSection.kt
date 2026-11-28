package dev.monarchui.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.forms.checkboxgroup.MonarchCheckboxGroupOption
import dev.monarchui.components.intelligence.aiconversation.MonarchAiMessageFeedback
import dev.monarchui.components.intelligence.aifeedback.MonarchAiFeedback
import dev.monarchui.components.intelligence.aifeedback.MonarchAiFeedbackDetails
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sampleicons.SampleIcons
import java.io.IOException
import kotlinx.coroutines.delay

private const val SimulatedSendMillis = 800L

/** The catalogue page of AiFeedback. */
object MonarchAiFeedbackSection : CataloguePage {
    override val title = "AiFeedback"

    @Composable
    override fun Content() {
        var rating by rememberSaveable { mutableStateOf<MonarchAiMessageFeedback?>(null) }
        var other by rememberSaveable { mutableStateOf<MonarchAiMessageFeedback?>(null) }
        var attempts by rememberSaveable { mutableIntStateOf(0) }
        CatalogueCaption("Thumbs down asks why; the first send fails")
        MonarchAiFeedback(selection = rating, onSelectionChange = { rating = it }) {
            delay(SimulatedSendMillis)
            if (attempts++ == 0) throw IOException("Offline")
        }
        CatalogueCaption("Icons from the app, both ratings ask")
        MonarchAiFeedback(
            selection = other,
            onSelectionChange = { other = it },
            reasons = mapOf(
                MonarchAiMessageFeedback.Up to listOf(MonarchCheckboxGroupOption("clear", "Clear")),
                MonarchAiMessageFeedback.Down to listOf(MonarchCheckboxGroupOption("wrong", "Wrong")),
            ),
            asksDetails = MonarchAiFeedbackDetails.Both,
            icon = { value, _ ->
                val glyph = if (value == MonarchAiMessageFeedback.Up) SampleIcons.ThumbUp else SampleIcons.ThumbDown
                MonarchIcon(imageVector = glyph, contentDescription = null)
            },
        ) { delay(SimulatedSendMillis) }
    }
}
