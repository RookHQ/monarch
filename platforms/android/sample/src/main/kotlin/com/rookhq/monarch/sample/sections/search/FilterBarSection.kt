@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.rookhq.monarch.sample.sections.search

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.datadisplay.tag.MonarchTag
import com.rookhq.monarch.components.overlays.bottomsheet.MonarchBottomSheet
import com.rookhq.monarch.components.search.facetedfilterpanel.MonarchFacet
import com.rookhq.monarch.components.search.facetedfilterpanel.MonarchFacetOption
import com.rookhq.monarch.components.search.facetedfilterpanel.MonarchFacetSelection
import com.rookhq.monarch.components.search.facetedfilterpanel.MonarchFacetedFilterPanel
import com.rookhq.monarch.components.search.searchbar.MonarchSearchBar
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.sampleicons.SampleIcons
import com.rookhq.monarch.theme.MonarchTheme

/** The catalogue page of FilterBar: a recipe of existing components. */
object FilterBarSection : CataloguePage {
    override val title = "FilterBar"

    @Composable
    override fun Content() {
        var query by rememberSaveable { mutableStateOf("") }
        var mine by rememberSaveable { mutableStateOf(false) }
        var sheet by rememberSaveable { mutableStateOf(false) }
        var filters by remember {
            mutableStateOf<Map<String, MonarchFacetSelection>>(
                mapOf("status" to MonarchFacetSelection.Options(setOf("open"))),
            )
        }
        val statuses = listOf(MonarchFacetOption("open", "Open", count = 12), MonarchFacetOption("closed", "Closed"))
        val facets = listOf(
            MonarchFacet.options("status", "Status", statuses),
            MonarchFacet.options(
                "priority",
                "Priority",
                listOf(MonarchFacetOption("high", "High", count = 4), MonarchFacetOption("low", "Low", count = 9)),
            ),
        )
        val chosen = (filters["status"] as? MonarchFacetSelection.Options)?.ids.orEmpty()
        val active = filters.values.count { it is MonarchFacetSelection.Options && it.ids.isNotEmpty() }
        CatalogueCaption("Search, quick filters, applied filters and the full set in a sheet")
        Column(verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2)) {
            MonarchSearchBar(
                query = query,
                onQueryChange = { query = it },
                onSearch = {},
                modifier = Modifier.fillMaxWidth(),
                placeholder = "Search tickets",
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AssistChip(
                    onClick = { sheet = true },
                    label = { Text(if (active > 0) "Filters ($active)" else "Filters") },
                )
                MonarchTag(text = "Assigned to me", selected = mine, onSelectedChange = { mine = it })
                statuses.filter { it.id in chosen }.forEach { option ->
                    MonarchTag(
                        text = "Status: ${option.label}",
                        onRemove = {
                            filters = filters + ("status" to MonarchFacetSelection.Options(chosen - option.id))
                        },
                        removeIcon = { MonarchIcon(SampleIcons.Close, contentDescription = null) },
                    )
                }
                if (active > 0) {
                    TextButton(onClick = { filters = emptyMap() }) { Text("Clear all") }
                }
            }
            MonarchText(
                text = if (mine) "4 results" else "12 results",
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        if (sheet) {
            MonarchBottomSheet(onDismissRequest = { sheet = false }) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    MonarchFacetedFilterPanel(facets = facets, value = filters, onValueChange = { filters = it }) {
                        MonarchButton(
                            text = "Show results",
                            onClick = { this@MonarchBottomSheet.dismiss() },
                            modifier = Modifier.fillMaxWidth(),
                            variant = MonarchButtonVariant.Primary,
                        )
                    }
                }
            }
        }
    }
}
