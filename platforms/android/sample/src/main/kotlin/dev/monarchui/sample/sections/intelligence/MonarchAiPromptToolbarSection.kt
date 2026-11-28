package dev.monarchui.sample.sections.intelligence

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.intelligence.aiprompttoolbar.MonarchAiPromptToolbar
import dev.monarchui.components.intelligence.aiprompttoolbar.MonarchAiPromptToolbarButton
import dev.monarchui.components.intelligence.aiprompttoolbar.MonarchAiPromptToolbarToggle
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sampleicons.SampleIcons

/** The catalogue page of AiPromptToolbar. */
object MonarchAiPromptToolbarSection : CataloguePage {
    override val title = "AiPromptToolbar"

    @Composable
    override fun Content() {
        var web by rememberSaveable { mutableStateOf(true) }
        var research by rememberSaveable { mutableStateOf(false) }
        var attachments by rememberSaveable { mutableIntStateOf(0) }
        CatalogueCaption("Buttons and toggles")
        MonarchAiPromptToolbar {
            MonarchAiPromptToolbarButton(
                text = "Attach",
                onClick = { attachments++ },
                icon = { Icon(SampleIcons.Attach, contentDescription = null) },
            )
            MonarchAiPromptToolbarToggle(text = "Search the web", checked = web, onCheckedChange = { web = it })
            MonarchAiPromptToolbarToggle(
                text = "Deep research",
                checked = research,
                onCheckedChange = { research = it },
            )
        }
        MonarchText("Attachments: $attachments")
        CatalogueCaption("Icon only, and disabled")
        MonarchAiPromptToolbar {
            MonarchAiPromptToolbarButton(
                text = "Attach",
                onClick = { attachments++ },
                hideLabel = true,
                icon = { Icon(SampleIcons.Attach, contentDescription = null) },
            )
            MonarchAiPromptToolbarToggle(text = "Think", checked = true, onCheckedChange = {}, enabled = false)
        }
    }
}
