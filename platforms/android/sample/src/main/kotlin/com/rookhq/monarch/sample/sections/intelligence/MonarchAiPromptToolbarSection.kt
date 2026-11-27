package com.rookhq.monarch.sample.sections.intelligence

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.intelligence.aiprompttoolbar.MonarchAiPromptToolbar
import com.rookhq.monarch.components.intelligence.aiprompttoolbar.MonarchAiPromptToolbarButton
import com.rookhq.monarch.components.intelligence.aiprompttoolbar.MonarchAiPromptToolbarToggle
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.sampleicons.SampleIcons

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
