@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.intelligence.aivoiceinput.MonarchAiVoiceInput
import dev.monarchui.components.intelligence.aivoiceinput.MonarchAiVoiceInputMode
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

/** The catalogue page of AiVoiceInput. It uses the device's speech recogniser, so it shows nothing without one. */
object MonarchAiVoiceInputSection : CataloguePage {
    override val title = "AiVoiceInput"

    @Composable
    override fun Content() {
        var prompt by rememberSaveable { mutableStateOf("") }
        CatalogueCaption("Press to start and stop")
        MonarchAiVoiceInput(onTranscript = { text, final ->
            if (final) prompt = listOf(prompt, text).filter { it.isNotEmpty() }.joinToString(" ")
        })
        CatalogueCaption("Hold to talk")
        MonarchAiVoiceInput(
            onTranscript = { text, final -> if (final) prompt = text },
            mode = MonarchAiVoiceInputMode.PushToTalk,
            size = MonarchButtonSize.Sm,
        )
        CatalogueCaption("Prompt")
        MonarchText(prompt.ifEmpty { "Nothing dictated yet." })
    }
}
