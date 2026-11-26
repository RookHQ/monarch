package com.rookhq.monarch.sample.sections.workflow

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.components.workflow.dependencypicker.MonarchDependencyItem
import com.rookhq.monarch.components.workflow.dependencypicker.MonarchDependencyLink
import com.rookhq.monarch.components.workflow.dependencypicker.MonarchDependencyPicker
import com.rookhq.monarch.components.workflow.dependencypicker.MonarchDependencyRelation
import com.rookhq.monarch.components.workflow.statuspicker.MonarchStatusCategory
import com.rookhq.monarch.components.workflow.statuspicker.MonarchStatusOption
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

/** The catalogue page of DependencyPicker: the links of one issue, with add, remove and open. */
object MonarchDependencyPickerSection : CataloguePage {
    override val title = "DependencyPicker"

    private val started = MonarchStatusOption("doing", "In progress", MonarchStatusCategory.Started)
    private val todo = MonarchStatusOption("todo", "To do", MonarchStatusCategory.Unstarted)
    private val done = MonarchStatusOption("done", "Done", MonarchStatusCategory.Completed)

    private val items = listOf(
        MonarchDependencyItem("1", "This issue", key = "STR-1", status = started),
        MonarchDependencyItem("12", "Fix the login redirect", key = "STR-12", status = started),
        MonarchDependencyItem("30", "Move billing to the new API", key = "STR-30", status = todo),
        MonarchDependencyItem("41", "Write the release notes", key = "STR-41", status = done),
        MonarchDependencyItem("44", "Release notes for 2.4", key = "STR-44", status = todo),
    )

    @Composable
    override fun Content() {
        var links by remember {
            mutableStateOf(listOf(MonarchDependencyLink("l1", MonarchDependencyRelation.BlockedBy, items[1])))
        }
        var next by remember { mutableIntStateOf(2) }
        var opened by remember { mutableStateOf<String?>(null) }
        CatalogueCaption("Links of STR-1")
        MonarchDependencyPicker(
            value = links,
            onAdd = { relation, item ->
                links = links + MonarchDependencyLink("l$next", relation, item)
                next += 1
            },
            onRemove = { link -> links = links.filterNot { it.id == link.id } },
            onOpen = { opened = it.key },
            items = items,
            excludeIds = setOf("1"),
        )
        opened?.let { MonarchText(text = "Opened $it.", color = MonarchTheme.colors.textMuted) }
        CatalogueCaption("Read only")
        MonarchDependencyPicker(value = links, readOnly = true)
    }
}
