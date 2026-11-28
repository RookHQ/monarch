@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.sample.sections.intelligence

import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.intelligence.aiconversation.MonarchAiAgentRef
import dev.monarchui.components.intelligence.aiconversationheader.MonarchAiConversationHeader
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sampleicons.SampleIcons
import kotlinx.coroutines.delay

private const val SampleSaveMillis = 600L

/** The catalogue page of AiConversationHeader. */
object MonarchAiConversationHeaderSection : CataloguePage {
    override val title = "AiConversationHeader"

    @Composable
    override fun Content() {
        var title by rememberSaveable { mutableStateOf("Q3 revenue summary") }
        CatalogueCaption("Tap the title to rename it")
        MonarchAiConversationHeader(
            title = title,
            agent = MonarchAiAgentRef(name = "Dossier"),
            onRename = { next ->
                delay(SampleSaveMillis)
                title = next
            },
            navigationIcon = {
                IconButton(onClick = {}) { MonarchIcon(SampleIcons.ArrowBack, contentDescription = "Back") }
            },
            model = { Text("Fast model") },
        ) {
            IconButton(onClick = {}) { MonarchIcon(SampleIcons.Plus, contentDescription = "New conversation") }
        }
        CatalogueCaption("While the title is generated")
        MonarchAiConversationHeader(title = "", naming = true, agent = MonarchAiAgentRef(name = "Dossier"))
        CatalogueCaption("Untitled, read only")
        MonarchAiConversationHeader(title = "")
    }
}
