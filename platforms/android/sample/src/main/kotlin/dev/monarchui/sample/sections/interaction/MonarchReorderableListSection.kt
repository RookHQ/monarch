package dev.monarchui.sample.sections.interaction

import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import dev.monarchui.components.interaction.reorderablelist.MonarchReorderableList
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

/** The catalogue page of ReorderableList: long press a row to drag it, or use the handle's actions. */
object MonarchReorderableListSection : CataloguePage {
    override val title = "ReorderableList"

    private const val HEIGHT_STEPS = 3

    @Composable
    override fun Content() {
        val tasks = remember { mutableStateListOf("Design review", "Budget", "Hiring plan", "Launch checklist") }
        val stages = remember { mutableStateListOf("Lead", "Qualified", "Proposal", "Won") }
        CatalogueCaption("Long press a row and drag it")
        MonarchReorderableList(
            items = tasks,
            key = { it },
            itemLabel = { it },
            onReorder = { from, to -> tasks.add(to, tasks.removeAt(from)) },
            modifier = Modifier.heightIn(max = MonarchTheme.spacing.space24 * HEIGHT_STEPS),
        ) { task, _ -> MonarchText(text = task) }
        CatalogueCaption("With move buttons; Won is locked")
        MonarchReorderableList(
            items = stages,
            key = { it },
            itemLabel = { it },
            onReorder = { from, to -> stages.add(to, stages.removeAt(from)) },
            modifier = Modifier.heightIn(max = MonarchTheme.spacing.space24 * HEIGHT_STEPS),
            itemEnabled = { it != "Won" },
            moveButtons = true,
        ) { stage, state -> MonarchText(text = "${state.index + 1}. $stage") }
    }
}
