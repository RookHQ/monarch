package dev.monarchui.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.intelligence.aicontextdivider.MonarchAiContextDivider
import dev.monarchui.components.intelligence.aicontextdivider.MonarchAiContextDividerKind
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

private const val DayMillis = 86_400_000L

/** The catalogue page of AiContextDivider. */
object MonarchAiContextDividerSection : CataloguePage {
    override val title = "AiContextDivider"

    @Composable
    override fun Content() {
        var showingEarlier by rememberSaveable { mutableStateOf(false) }
        val now = System.currentTimeMillis()
        CatalogueCaption("Days")
        MonarchAiContextDivider(kind = MonarchAiContextDividerKind.Day, dateMillis = now)
        MonarchAiContextDivider(kind = MonarchAiContextDividerKind.Day, dateMillis = now - DayMillis)
        MonarchAiContextDivider(kind = MonarchAiContextDividerKind.Day, dateMillis = now - DayMillis * 5)
        CatalogueCaption("Context reset")
        MonarchAiContextDivider(kind = MonarchAiContextDividerKind.ContextReset)
        CatalogueCaption("Summarized, with the summary and a way back")
        if (showingEarlier) MonarchText(text = "The earlier messages are shown here.")
        MonarchAiContextDivider(
            kind = MonarchAiContextDividerKind.Summarized,
            summary = "The team agreed to ship the report on Friday and to drop the appendix.",
            onExpand = { showingEarlier = true },
        )
    }
}
