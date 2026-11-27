package com.rookhq.monarch.sample.sections.datagrid

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.overlays.menu.MonarchMenu
import com.rookhq.monarch.components.overlays.menu.MonarchMenuDivider
import com.rookhq.monarch.components.overlays.menu.MonarchMenuItem
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

/** The catalogue page of ColumnMenu: a Columns menu over a list of contacts that keeps one column visible. */
object ColumnMenuSection : CataloguePage {
    override val title = "ColumnMenu"

    private val columns = listOf("name" to "Name", "email" to "Email", "plan" to "Plan", "created" to "Created")

    @Composable
    override fun Content() {
        var visible by rememberSaveable { mutableStateOf(listOf("name", "email", "plan")) }
        var expanded by rememberSaveable { mutableStateOf(false) }
        CatalogueCaption("Show and hide columns")
        Box {
            MonarchButton(
                text = "Columns",
                onClick = { expanded = true },
                modifier = Modifier.semantics { stateDescription = "${visible.size} of ${columns.size} shown" },
                size = MonarchButtonSize.Sm,
            )
            MonarchMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                columns.forEach { (id, title) ->
                    val shown = id in visible
                    MonarchMenuItem(
                        text = title,
                        onClick = { visible = if (shown) visible - id else visible + id },
                        enabled = !(shown && visible.size == 1),
                        selected = shown,
                    )
                }
                MonarchMenuDivider()
                MonarchMenuItem(
                    text = "Show all columns",
                    onClick = {
                        visible = columns.map { it.first }
                        expanded = false
                    },
                    enabled = visible.size < columns.size,
                )
            }
        }
        val shown = columns.filter { it.first in visible }.joinToString(" · ") { it.second }
        MonarchText(text = "Table columns: $shown", color = MonarchTheme.colors.textMuted)
    }
}
