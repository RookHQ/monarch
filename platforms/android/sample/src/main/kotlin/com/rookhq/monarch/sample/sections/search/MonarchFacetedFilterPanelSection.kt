package com.rookhq.monarch.sample.sections.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.overlays.bottomsheet.MonarchBottomSheet
import com.rookhq.monarch.components.search.facetedfilterpanel.MonarchFacet
import com.rookhq.monarch.components.search.facetedfilterpanel.MonarchFacetOption
import com.rookhq.monarch.components.search.facetedfilterpanel.MonarchFacetSelection
import com.rookhq.monarch.components.search.facetedfilterpanel.MonarchFacetedFilterPanel
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.sampleicons.SampleIcons

/** The catalogue page of FacetedFilterPanel: in a bottom sheet, as on a phone, and inline. */
object MonarchFacetedFilterPanelSection : CataloguePage {
    override val title = "FacetedFilterPanel"

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val facets = remember {
            listOf(
                MonarchFacet.options(
                    id = "status",
                    label = "Status",
                    options = listOf(
                        MonarchFacetOption("open", "Open", count = 128),
                        MonarchFacetOption("review", "In review", count = 42),
                        MonarchFacetOption("done", "Done", count = 1_204),
                        MonarchFacetOption("archived", "Archived", count = 0, enabled = false),
                    ),
                    maxVisible = 3,
                ),
                MonarchFacet.options(
                    id = "owner",
                    label = "Owner",
                    options = listOf(
                        MonarchFacetOption("alex", "Alex Kim", count = 64),
                        MonarchFacetOption("sam", "Sam Rivera", count = 51),
                    ),
                    initiallyExpanded = false,
                ),
                MonarchFacet.range("size", "File size", bounds = 0f..100f, step = 5f, unit = "MB"),
            )
        }
        var filters by remember { mutableStateOf<Map<String, MonarchFacetSelection>>(emptyMap()) }
        var open by rememberSaveable { mutableStateOf(false) }
        CatalogueCaption("In a bottom sheet")
        MonarchButton(text = "Filters", onClick = { open = true })
        if (open) {
            MonarchBottomSheet(onDismissRequest = { open = false }) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    MonarchFacetedFilterPanel(
                        facets = facets,
                        value = filters,
                        onValueChange = { filters = it },
                        expandIcon = {
                            MonarchIcon(SampleIcons.ChevronDown, contentDescription = null)
                        },
                    ) {
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
        CatalogueCaption("Inline, without a title")
        MonarchFacetedFilterPanel(facets = facets, value = filters, onValueChange = { filters = it }, showTitle = false)
    }
}
