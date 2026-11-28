package dev.monarchui.sample.sections.charts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.charts.barlist.MonarchBarList
import dev.monarchui.components.charts.barlist.MonarchBarListItem
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

/** The catalogue page of BarList. */
object MonarchBarListSection : CataloguePage {
    override val title = "BarList"

    private val pages = listOf(
        MonarchBarListItem("/pricing", 4210.0),
        MonarchBarListItem("/docs", 2980.0, description = "Documentation"),
        MonarchBarListItem("/blog", 1120.0),
        MonarchBarListItem("/changelog", 640.0),
        MonarchBarListItem("/legacy", null),
    )
    private val channels = listOf(
        MonarchBarListItem("Email", 0.12, displayValue = "12%"),
        MonarchBarListItem("Search", 0.31, displayValue = "31%", highlighted = true),
        MonarchBarListItem("Social", 0.07, displayValue = "7%"),
    )

    @Composable
    override fun Content() {
        var selected by rememberSaveable { mutableStateOf("None") }
        CatalogueCaption("Ranked, limited to three")
        MonarchBarList(items = pages, limit = 3, ranked = true, label = "Top pages")
        CatalogueCaption("Ratios of a whole, selectable")
        MonarchBarList(
            items = channels,
            maximum = 1.0,
            label = "Conversion by channel",
            onSelect = { selected = it.label },
        )
        MonarchText(text = "Selected: $selected", color = MonarchTheme.colors.textMuted)
        CatalogueCaption("Empty and loading")
        MonarchBarList(items = emptyList())
        MonarchBarList(items = pages, loading = true)
    }
}
