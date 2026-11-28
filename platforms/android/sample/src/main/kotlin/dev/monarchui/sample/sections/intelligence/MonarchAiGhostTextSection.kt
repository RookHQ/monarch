package dev.monarchui.sample.sections.intelligence

import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import dev.monarchui.components.intelligence.aighosttext.MonarchAiGhostText
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

/** The catalogue page of AiGhostText, with a stand-in model that completes a few phrases. */
object MonarchAiGhostTextSection : CataloguePage {
    override val title = "AiGhostText"

    private val continuations = mapOf(
        "i will" to " send the figures by Friday.",
        "thanks for" to " the update.",
        "could you" to " share the latest version?",
    )

    /** A stand-in for a model: completes a phrase when the text ends with one of a few openings. */
    private fun complete(text: String): String? {
        val end = text.trimEnd().lowercase()
        return continuations.entries.firstOrNull { end.endsWith(it.key) }?.value
    }

    @Composable
    override fun Content() {
        CatalogueCaption("Type \"Thanks for\", \"Could you\" or \"I will\"")
        MonarchAiGhostText(
            state = rememberTextFieldState(),
            label = "Reply",
            helperText = "Tap the suggestion or Accept suggestion to use it.",
        ) { text, _ -> complete(text) }
        CatalogueCaption("Disabled")
        MonarchAiGhostText(state = rememberTextFieldState("Thanks"), label = "Reply", enabled = false) { _, _ -> null }
    }
}
