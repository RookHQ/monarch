package com.rookhq.monarch.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.intelligence.aicontextchips.MonarchAiContextChips
import com.rookhq.monarch.components.intelligence.aicontextchips.MonarchAiContextItem
import com.rookhq.monarch.components.intelligence.aicontextchips.MonarchAiContextKind
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.foundation.icon.MonarchIconSize
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.sampleicons.SampleIcons

private val AllItems = listOf(
    MonarchAiContextItem("contract", MonarchAiContextKind.File, "contract.pdf", tokens = 1200),
    MonarchAiContextItem("rows", MonarchAiContextKind.Selection, "Selected rows", tokens = 300),
    MonarchAiContextItem("ticket", MonarchAiContextKind.Record, "Ticket 4821", tokens = 5400),
    MonarchAiContextItem("ana", MonarchAiContextKind.Person, "Ana Silva", tokens = 80),
    MonarchAiContextItem("page", MonarchAiContextKind.Page, "Q3 planning", tokens = 2100, removable = false),
)

/** The catalogue page of AiContextChips. */
object MonarchAiContextChipsSection : CataloguePage {
    override val title = "AiContextChips"

    @Composable
    override fun Content() {
        var kept by rememberSaveable { mutableStateOf(AllItems.map { it.id }) }
        CatalogueCaption("Tap a chip to remove it. The page cannot be removed.")
        MonarchAiContextChips(
            items = AllItems.filter { it.id in kept },
            onRemove = { id -> kept = kept - id },
            kindIcon = { MonarchIcon(SampleIcons.Document, contentDescription = null, size = MonarchIconSize.Sm) },
            emptyText = "No context added",
        )
        MonarchButton(text = "Reset", onClick = { kept = AllItems.map { it.id } }, size = MonarchButtonSize.Sm)
        CatalogueCaption("Against a budget of 128K tokens")
        MonarchAiContextChips(items = AllItems, budget = 128_000)
        CatalogueCaption("Disabled while the answer is generated")
        MonarchAiContextChips(items = AllItems.take(2), enabled = false, onRemove = {})
    }
}
