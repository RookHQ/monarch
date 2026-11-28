package dev.monarchui.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.intelligence.aicontextwindowmeter.MonarchAiContextWindowMeter
import dev.monarchui.components.intelligence.aicontextwindowmeter.MonarchAiContextWindowPart
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

/** The catalogue page of AiContextWindowMeter. */
object MonarchAiContextWindowMeterSection : CataloguePage {
    override val title = "AiContextWindowMeter"

    @Composable
    override fun Content() {
        var messages by rememberSaveable { mutableLongStateOf(StartingMessages) }
        CatalogueCaption("What fills the window")
        MonarchAiContextWindowMeter(
            used = Instructions + Files + messages,
            limit = Limit,
            breakdown = listOf(
                MonarchAiContextWindowPart("Instructions", Instructions),
                MonarchAiContextWindowPart("Files", Files),
                MonarchAiContextWindowPart("Messages", messages),
            ),
            onSummarize = { messages = SummarizedMessages },
        )
        MonarchButton(text = "Add a long reply", onClick = { messages += Reply }, size = MonarchButtonSize.Sm)
        CatalogueCaption("Single bar, estimated")
        MonarchAiContextWindowMeter(used = 45_000, limit = Limit, approximate = true)
        CatalogueCaption("Compact, for a toolbar")
        MonarchAiContextWindowMeter(used = Instructions + Files + messages, limit = Limit, compact = true)
    }
}

private const val Limit = 128_000L
private const val Instructions = 4_000L
private const val Files = 36_000L
private const val StartingMessages = 40_000L
private const val SummarizedMessages = 6_000L
private const val Reply = 12_000L
