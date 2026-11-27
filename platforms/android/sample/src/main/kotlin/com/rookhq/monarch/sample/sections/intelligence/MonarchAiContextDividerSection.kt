package com.rookhq.monarch.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.intelligence.aicontextdivider.MonarchAiContextDivider
import com.rookhq.monarch.components.intelligence.aicontextdivider.MonarchAiContextDividerKind
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

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
