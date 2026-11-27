@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.sample.sections.collaboration

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.collaboration.linkeddocspanel.MonarchLinkedDocsPanel
import com.rookhq.monarch.components.collaboration.linkeddocspanel.MonarchLinkedDocsPanelItem
import com.rookhq.monarch.components.collaboration.linkeddocspanel.MonarchLinkedDocsPanelKind
import com.rookhq.monarch.components.collaboration.linkeddocspanel.MonarchLinkedDocsPanelOption
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

/** The catalogue page of LinkedDocsPanel: link a doc, add a link, filter by Space and unlink. */
object MonarchLinkedDocsPanelSection : CataloguePage {
    override val title = "LinkedDocsPanel"

    private val Docs = listOf(
        MonarchLinkedDocsPanelOption("d1", "Release runbook", "Engineering"),
        MonarchLinkedDocsPanelOption("d2", "Launch brief", "Marketing"),
        MonarchLinkedDocsPanelOption("d3", "Incident review template", "Engineering"),
    )

    private val Spaces = mapOf("Engineering" to "eng", "Marketing" to "mkt")

    @Composable
    override fun Content() {
        // Each linked row as "id|title|docId|url", so the list survives a configuration change.
        var rows by rememberSaveable {
            mutableStateOf(listOf("l1|Release runbook|d1|", "l2|Design file||https://design.example.com/file/123"))
        }
        var next by rememberSaveable { mutableIntStateOf(rows.size) }
        val items = rows.map { row ->
            val parts = row.split("|")
            val docId = parts[2]
            val url = parts[parts.lastIndex]
            val space = Docs.firstOrNull { it.docId == docId }?.description
            MonarchLinkedDocsPanelItem(
                id = parts.first(),
                title = parts[1],
                kind = if (docId.isEmpty()) MonarchLinkedDocsPanelKind.Link else MonarchLinkedDocsPanelKind.Doc,
                url = url.ifEmpty { null },
                docId = docId.ifEmpty { null },
                spaceId = space?.let { Spaces[it] },
                spaceName = space,
            )
        }
        CatalogueCaption("Docs and links on a ticket")
        MonarchLinkedDocsPanel(
            items = items,
            onOpen = {},
            docOptions = Docs,
            onLinkDoc = { docId ->
                next += 1
                rows = rows + "l$next|${Docs.first { it.docId == docId }.label}|$docId|"
            },
            onAddLink = { url, title ->
                next += 1
                rows = rows + "l$next|$title||$url"
            },
            onUnlink = { item -> rows = rows.filterNot { it.startsWith("${item.id}|") } },
        )
        CatalogueCaption("Loading and read-only")
        MonarchLinkedDocsPanel(items = null, onOpen = {})
        MonarchLinkedDocsPanel(items = items.take(1), onOpen = {}, readOnly = true)
    }
}
