package dev.monarchui.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.intelligence.aitonepicker.MonarchAiTonePicker
import dev.monarchui.components.intelligence.aitonepicker.MonarchAiTonePickerVariant
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

/** The catalogue page of AiTonePicker. */
object MonarchAiTonePickerSection : CataloguePage {
    override val title = "AiTonePicker"

    @Composable
    override fun Content() {
        var tone by rememberSaveable { mutableStateOf("professional") }
        var custom by rememberSaveable { mutableStateOf("") }
        CatalogueCaption("Cards, with a custom tone")
        MonarchAiTonePicker(
            selection = tone,
            onSelectionChange = { tone = it },
            customText = custom,
            onCustomTextChange = { custom = it },
        )
        CatalogueCaption("Compact")
        MonarchAiTonePicker(
            selection = tone,
            onSelectionChange = { tone = it },
            variant = MonarchAiTonePickerVariant.Compact,
        )
    }
}
