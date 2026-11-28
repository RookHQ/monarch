package dev.monarchui.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.intelligence.aiconversation.MonarchAiPartState
import dev.monarchui.components.intelligence.aiconversation.MonarchAiReasoningPart
import dev.monarchui.components.intelligence.aireasoning.MonarchAiReasoning
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sampleicons.SampleIcons

private const val SampleReasoning = "The question asks for churn last quarter. The table has monthly rows, so I add " +
    "July, August and September, then divide by the customers at the start of July."

/** The catalogue page of AiReasoning. */
object MonarchAiReasoningSection : CataloguePage {
    override val title = "AiReasoning"

    @Composable
    override fun Content() {
        var streaming by rememberSaveable { mutableStateOf(true) }
        CatalogueCaption("Streaming, then finished: it closes by itself unless you opened or closed it")
        MonarchAiReasoning(
            part = MonarchAiReasoningPart(
                text = SampleReasoning,
                state = if (streaming) MonarchAiPartState.Streaming else MonarchAiPartState.Done,
            ),
            expandIcon = { MonarchIcon(imageVector = SampleIcons.ChevronRight, contentDescription = null) },
        )
        MonarchButton(text = if (streaming) "Finish" else "Stream again", onClick = { streaming = !streaming })
        CatalogueCaption("Finished with a known duration")
        MonarchAiReasoning(part = MonarchAiReasoningPart(text = SampleReasoning, durationMs = 12_000L))
        CatalogueCaption("Redacted")
        MonarchAiReasoning(
            part = MonarchAiReasoningPart(text = "", redacted = true),
            icon = { MonarchIcon(imageVector = SampleIcons.Bot, contentDescription = null) },
        )
    }
}
