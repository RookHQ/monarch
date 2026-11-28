package dev.monarchui.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.intelligence.aimodeswitcher.MonarchAiMode
import dev.monarchui.components.intelligence.aimodeswitcher.MonarchAiModeSwitcher
import dev.monarchui.components.intelligence.aimodeswitcher.MonarchAiModeSwitcherSize
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sampleicons.SampleIcons

/** The catalogue page of AiModeSwitcher. */
object MonarchAiModeSwitcherSection : CataloguePage {
    override val title = "AiModeSwitcher"

    @Composable
    override fun Content() {
        var mode by rememberSaveable { mutableStateOf("ask") }
        var own by rememberSaveable { mutableStateOf("research") }
        CatalogueCaption("Ask, Edit and Agent")
        MonarchAiModeSwitcher(selection = mode, onSelectionChange = { mode = it })
        CatalogueCaption("Own modes, medium, one not set up")
        MonarchAiModeSwitcher(
            selection = own,
            onSelectionChange = { own = it },
            size = MonarchAiModeSwitcherSize.Md,
            modes = listOf(
                MonarchAiMode(
                    id = "research",
                    label = "Research",
                    description = "Reads sources and cites them.",
                    icon = { MonarchIcon(SampleIcons.Search, contentDescription = null) },
                ),
                MonarchAiMode("write", "Write", "Drafts text for you to edit."),
                MonarchAiMode("automate", "Automate", "Not set up in this workspace.", enabled = false),
            ),
        )
        CatalogueCaption("Disabled while a response runs")
        MonarchAiModeSwitcher(selection = "agent", onSelectionChange = {}, enabled = false)
    }
}
