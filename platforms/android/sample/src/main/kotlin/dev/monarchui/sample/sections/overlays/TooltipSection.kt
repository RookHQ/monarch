package dev.monarchui.sample.sections.overlays

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sampleicons.SampleIcons
import kotlinx.coroutines.launch

/** The catalogue page of Tooltip: PlainTooltip in a TooltipBox, shown on a long press. */
object TooltipSection : CataloguePage {
    override val title = "Tooltip"

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val favourite = rememberTooltipState()
        val search = rememberTooltipState(isPersistent = true)
        val scope = rememberCoroutineScope()
        CatalogueCaption("Touch and hold the star")
        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
            tooltip = { PlainTooltip { Text("Add to favourites") } },
            state = favourite,
        ) {
            IconButton(onClick = {}) {
                Icon(SampleIcons.Star, contentDescription = "Add to favourites")
            }
        }
        CatalogueCaption("Shown from code")
        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Below),
            tooltip = { PlainTooltip { Text("Search invoices by number or customer") } },
            state = search,
        ) {
            IconButton(onClick = {}) {
                Icon(SampleIcons.Search, contentDescription = "Search")
            }
        }
        MonarchButton(
            text = "Show the hint",
            onClick = { scope.launch { search.show() } },
            variant = MonarchButtonVariant.Ghost,
        )
    }
}
