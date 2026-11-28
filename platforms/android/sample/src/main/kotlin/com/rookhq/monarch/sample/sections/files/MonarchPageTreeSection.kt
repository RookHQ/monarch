package com.rookhq.monarch.sample.sections.files

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.rookhq.monarch.components.files.pagetree.MonarchPageTree
import com.rookhq.monarch.components.files.pagetree.MonarchPageTreeGroup
import com.rookhq.monarch.components.files.pagetree.MonarchPageTreePage
import com.rookhq.monarch.components.forms.switchrow.MonarchSwitchRow
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

private const val TreeHeightSteps = 5

/** The catalogue page of PageTree: a handbook's pages, as navigation or as an editable organiser. */
object MonarchPageTreeSection : CataloguePage {
    override val title = "PageTree"

    private val start = listOf(
        MonarchPageTreeGroup(
            id = "basics",
            label = "Basics",
            pages = listOf(MonarchPageTreePage("welcome", "Welcome"), MonarchPageTreePage("setup", "Set up")),
        ),
        MonarchPageTreeGroup(
            id = "guides",
            label = "Guides",
            pages = listOf(MonarchPageTreePage("billing", "Billing"), MonarchPageTreePage("teams", "Teams")),
        ),
    )

    @Composable
    override fun Content() {
        var editing by rememberSaveable { mutableStateOf(false) }
        var page by rememberSaveable { mutableStateOf<String?>("welcome") }
        var groups by remember { mutableStateOf(start) }
        var created by rememberSaveable { mutableIntStateOf(0) }
        CatalogueCaption("Handbook")
        MonarchSwitchRow(checked = editing, onCheckedChange = { editing = it }, label = "Organise pages")
        val title = groups.flatMap { it.pages }.firstOrNull { it.id == page }?.title
        MonarchText(text = "Open: ${title ?: "nothing"}", color = MonarchTheme.colors.textMuted)
        val tree = Modifier.fillMaxWidth().height(MonarchTheme.spacing.space24 * TreeHeightSteps)
        if (!editing) {
            MonarchPageTree(groups = groups, selected = page, onSelect = { page = it }, modifier = tree)
        } else {
            MonarchPageTree(
                groups = groups,
                selected = page,
                onSelect = { page = it },
                modifier = tree,
                onReorder = { next, _ -> groups = next },
                onAddPage = { groupId ->
                    created++
                    val added = MonarchPageTreePage("new-$created", "New page $created")
                    groups = groups.map {
                        if (it.id == groupId) MonarchPageTreeGroup(it.id, it.label, it.pages + added) else it
                    }
                },
                onAddGroup = { label ->
                    created++
                    groups = groups + MonarchPageTreeGroup("group-$created", label, emptyList())
                },
                onRenameGroup = { id, label ->
                    groups = groups.map { if (it.id == id) MonarchPageTreeGroup(id, label, it.pages) else it }
                },
                onDeleteGroup = { id -> groups = groups.filterNot { it.id == id } },
                onDeletePage = { id ->
                    groups = groups.map { group ->
                        MonarchPageTreeGroup(group.id, group.label, group.pages.filterNot { it.id == id })
                    }
                },
            )
        }
    }
}
