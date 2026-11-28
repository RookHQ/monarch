package dev.monarchui.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.intelligence.aicontextchips.MonarchAiContextChips
import dev.monarchui.components.intelligence.aicontextchips.MonarchAiContextItem
import dev.monarchui.components.intelligence.aicontextchips.MonarchAiContextKind
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.foundation.icon.MonarchIconSize
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sampleicons.SampleIcons

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
