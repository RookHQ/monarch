package dev.monarchui.sample.sections.actions

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.monarchui.components.datadisplay.tag.MonarchChip
import dev.monarchui.components.overlays.menu.MonarchMenu
import dev.monarchui.components.overlays.menu.MonarchMenuItem
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sampleicons.SampleIcons
import dev.monarchui.theme.MonarchTheme

/** The catalogue page of Toolbar: the Material 3 app bars hold the controls above a list. */
object ToolbarSection : CataloguePage {
    override val title = "Toolbar"

    private val filters = listOf("Open", "Overdue", "Paid")

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        var menu by rememberSaveable { mutableStateOf(false) }
        var last by rememberSaveable { mutableStateOf("Nothing yet") }
        var selected by rememberSaveable { mutableStateOf("Open") }
        CatalogueCaption("TopAppBar with actions and an overflow menu, then filter chips")
        TopAppBar(
            title = { Text("Invoices") },
            actions = {
                IconButton(onClick = { last = "Search" }) {
                    Icon(SampleIcons.Search, contentDescription = "Search")
                }
                Box {
                    IconButton(onClick = { menu = true }) {
                        Icon(SampleIcons.MoreVertical, contentDescription = "More actions")
                    }
                    MonarchMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        listOf("Export", "Density").forEach { action ->
                            MonarchMenuItem(
                                text = action,
                                onClick = {
                                    last = action
                                    menu = false
                                },
                            )
                        }
                    }
                }
            },
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2),
        ) {
            filters.forEach { filter ->
                MonarchChip(text = filter, selected = selected == filter, onSelectedChange = { selected = filter })
            }
        }
        CatalogueCaption("BottomAppBar for actions in reach of the thumb")
        BottomAppBar(
            actions = {
                IconButton(onClick = { last = "Share" }) {
                    Icon(SampleIcons.Share, contentDescription = "Share")
                }
                IconButton(onClick = { last = "Sort" }) {
                    Icon(SampleIcons.Sort, contentDescription = "Sort")
                }
            },
        )
        MonarchText(text = "Filter: $selected. Last action: $last", color = MonarchTheme.colors.textMuted)
    }
}
