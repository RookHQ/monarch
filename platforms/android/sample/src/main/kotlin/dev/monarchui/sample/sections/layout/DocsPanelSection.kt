@file:OptIn(
    ExperimentalMonarchApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package dev.monarchui.sample.sections.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.collaboration.linkeddocspanel.MonarchLinkedDocsPanel
import dev.monarchui.components.collaboration.linkeddocspanel.MonarchLinkedDocsPanelItem
import dev.monarchui.components.collaboration.linkeddocspanel.MonarchLinkedDocsPanelKind
import dev.monarchui.components.datadisplay.collapsible.MonarchCollapsible
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

/** The catalogue page of DocsPanel: a recipe of existing components. */
object DocsPanelSection : CataloguePage {
    override val title = "DocsPanel"

    @Composable
    override fun Content() {
        CatalogueCaption("Docs behind a toggle, loaded when opened")
        run {
            var expanded by remember { mutableStateOf(false) }
            var items by remember { mutableStateOf<List<MonarchLinkedDocsPanelItem>?>(null) }
            MonarchCollapsible(
                expanded = expanded,
                onExpandedChange = { expanded = it },
                label = "Docs, 2 linked",
            ) {
                // Composed only while open: the app loads the docs here, not for every record.
                LaunchedEffect(Unit) {
                    items = listOf(
                        MonarchLinkedDocsPanelItem(
                            id = "l1",
                            title = "Release runbook",
                            docId = "d1",
                            meta = "Updated 2 hours ago",
                        ),
                        MonarchLinkedDocsPanelItem(
                            id = "l2",
                            title = "Design file",
                            kind = MonarchLinkedDocsPanelKind.Link,
                            url = "https://design.example.com/f/1",
                        ),
                    )
                }
                MonarchLinkedDocsPanel(
                    items = items,
                    onOpen = { /* Open the doc or the link. */ },
                    onUnlink = { item -> items = items?.minus(item) },
                )
            }
        }
        CatalogueCaption("Docs in a record column")
        run {
            MonarchLinkedDocsPanel(
                items = listOf(
                    MonarchLinkedDocsPanelItem(id = "l1", title = "Customer contract", docId = "d7", meta = "Legal"),
                ),
                onOpen = {},
                readOnly = true,
            )
        }
    }
}
