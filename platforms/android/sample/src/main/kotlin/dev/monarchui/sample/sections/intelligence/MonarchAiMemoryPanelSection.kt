@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.sample.sections.intelligence

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.intelligence.aimemorypanel.MonarchAiMemoryEntity
import dev.monarchui.components.intelligence.aimemorypanel.MonarchAiMemoryEntityKind
import dev.monarchui.components.intelligence.aimemorypanel.MonarchAiMemoryItem
import dev.monarchui.components.intelligence.aimemorypanel.MonarchAiMemoryPanel
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

private val PanelHeight = 560.dp
private const val Day = 86_400_000L
private const val Week = 7 * Day

private fun sampleMemories(now: Long): List<MonarchAiMemoryItem> {
    val ada = MonarchAiMemoryEntity(MonarchAiMemoryEntityKind.Person, "Ada Lovelace")
    val acme = MonarchAiMemoryEntity(MonarchAiMemoryEntityKind.Organization, "Acme")
    val atlas = MonarchAiMemoryEntity(MonarchAiMemoryEntityKind.Project, "Atlas")
    return listOf(
        MonarchAiMemoryItem("m1", "Prefers summaries under 200 words", ada, now),
        MonarchAiMemoryItem("m2", "Works in the Lisbon office", ada, now - Day),
        MonarchAiMemoryItem("m3", "Renewal is due in June", acme, now - 2 * Day),
        MonarchAiMemoryItem("m4", "Launch moved to the second week of May", atlas, now - Week),
    )
}

/** The catalogue page of AiMemoryPanel. */
object MonarchAiMemoryPanelSection : CataloguePage {
    override val title = "AiMemoryPanel"

    @Composable
    override fun Content() {
        var memories by remember { mutableStateOf(sampleMemories(System.currentTimeMillis())) }
        CatalogueCaption("Search, edit, forget one, or select several and forget them after a confirmation")
        MonarchAiMemoryPanel(
            memories = memories,
            onEdit = { id, text ->
                val now = System.currentTimeMillis()
                memories = memories.map { if (it.id == id) it.copy(text = text, updatedAt = now) else it }
            },
            onForget = { id -> memories = memories.filterNot { it.id == id } },
            onForgetMany = { ids -> memories = memories.filterNot { it.id in ids } },
            modifier = Modifier.height(PanelHeight),
        )
    }
}
