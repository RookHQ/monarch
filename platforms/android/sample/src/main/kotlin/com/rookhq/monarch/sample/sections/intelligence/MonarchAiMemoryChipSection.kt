package com.rookhq.monarch.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiMemoryAction
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiMemoryPart
import com.rookhq.monarch.components.intelligence.aimemorychip.MonarchAiMemoryChip
import com.rookhq.monarch.components.intelligence.aimemorychip.MonarchAiMemoryChipVariant
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

/** The catalogue page of AiMemoryChip. */
object MonarchAiMemoryChipSection : CataloguePage {
    override val title = "AiMemoryChip"

    @Composable
    override fun Content() {
        var text by rememberSaveable { mutableStateOf("Prefers summaries under 200 words") }
        var forgotten by rememberSaveable { mutableStateOf(false) }
        val action = if (forgotten) MonarchAiMemoryAction.Forget else MonarchAiMemoryAction.Write
        CatalogueCaption("A memory saved in this reply; tap it to edit or forget")
        MonarchAiMemoryChip(
            memory = MonarchAiMemoryPart(id = "m1", action = action, text = text, scope = "You"),
            onEdit = { _, new -> text = new },
            onForget = { forgotten = true },
        )
        CatalogueCaption("A memory the reply used, read only")
        MonarchAiMemoryChip(
            memory = MonarchAiMemoryPart(id = "m2", action = MonarchAiMemoryAction.Read, text = "Works in Lisbon"),
        )
        CatalogueCaption("Rows, for a list of memories")
        MonarchAiMemoryChip(
            memory = MonarchAiMemoryPart(id = "m3", action = MonarchAiMemoryAction.Write, text = "Team: Payments"),
            variant = MonarchAiMemoryChipVariant.Row,
            onEdit = { _, _ -> },
            onForget = {},
        )
        MonarchAiMemoryChip(
            memory = MonarchAiMemoryPart(id = "m4", action = MonarchAiMemoryAction.Forget, text = "Prefers tables"),
            variant = MonarchAiMemoryChipVariant.Row,
        )
    }
}
