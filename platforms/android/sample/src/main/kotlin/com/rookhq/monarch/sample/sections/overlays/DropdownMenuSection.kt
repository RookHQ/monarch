package com.rookhq.monarch.sample.sections.overlays

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.overlays.menu.MonarchMenu
import com.rookhq.monarch.components.overlays.menu.MonarchMenuDivider
import com.rookhq.monarch.components.overlays.menu.MonarchMenuItem
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

/** The catalogue page of DropdownMenu: a MonarchMenu from a MonarchButton. */
object DropdownMenuSection : CataloguePage {
    override val title = "DropdownMenu"

    @Composable
    override fun Content() {
        var expanded by rememberSaveable { mutableStateOf(false) }
        var showArchived by rememberSaveable { mutableStateOf(false) }
        var last by rememberSaveable { mutableStateOf<String?>(null) }
        CatalogueCaption("Actions, a checkable item and a danger item")
        Box {
            MonarchButton(text = "Actions", onClick = { expanded = true })
            MonarchMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                listOf("Edit", "Duplicate").forEach { action ->
                    MonarchMenuItem(
                        text = action,
                        onClick = {
                            last = action
                            expanded = false
                        },
                    )
                }
                MonarchMenuItem(text = "Move", onClick = {}, enabled = false)
                MonarchMenuItem(
                    text = "Show archived",
                    onClick = { showArchived = !showArchived },
                    selected = showArchived,
                )
                MonarchMenuDivider()
                MonarchMenuItem(
                    text = "Delete",
                    onClick = {
                        last = "Delete"
                        expanded = false
                    },
                    destructive = true,
                )
            }
        }
        last?.let { MonarchText(text = "Last action: $it") }
    }
}
