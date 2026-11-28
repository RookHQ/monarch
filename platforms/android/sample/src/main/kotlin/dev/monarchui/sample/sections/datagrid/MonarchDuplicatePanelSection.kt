package dev.monarchui.sample.sections.datagrid

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.datagrid.duplicatepanel.MonarchDuplicateMatch
import dev.monarchui.components.datagrid.duplicatepanel.MonarchDuplicatePanel
import dev.monarchui.components.datagrid.duplicatepanel.MonarchDuplicatePanelVariant
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

/** The catalogue page of DuplicatePanel. */
object MonarchDuplicatePanelSection : CataloguePage {
    override val title = "DuplicatePanel"

    private val matches = listOf(
        MonarchDuplicateMatch(
            id = "1",
            title = "Example Ltd",
            kind = "Client",
            reasons = listOf("Same email", "Same company domain"),
            score = 0.92f,
        ),
        MonarchDuplicateMatch(id = "2", title = "Example Limited", kind = "Open lead", detail = "Archived"),
    )

    @Composable
    override fun Content() {
        var dismissed by rememberSaveable { mutableStateOf(listOf<String>()) }
        var last by rememberSaveable { mutableStateOf("") }
        CatalogueCaption("Hint, under a create form")
        MonarchDuplicatePanel(
            matches = matches.filterNot { it.id in dismissed },
            footer = "You can still create it.",
            onOpen = { last = "Opened ${it.title}" },
            onMerge = { last = "Merging with ${it.title}" },
            onDismiss = { dismissed = dismissed + it.id },
        )
        if (last.isNotEmpty()) MonarchText(text = last, color = MonarchTheme.colors.textMuted)
        CatalogueCaption("Panel, on a record screen")
        MonarchDuplicatePanel(matches = matches, variant = MonarchDuplicatePanelVariant.Panel, onOpen = {})
    }
}
