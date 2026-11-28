package dev.monarchui.sample.sections.technical

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.monarchui.components.technical.jsonviewer.MonarchJsonValue
import dev.monarchui.components.technical.jsonviewer.MonarchJsonViewer
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.foundation.icon.MonarchIconSize
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sampleicons.SampleIcons
import dev.monarchui.theme.MonarchTheme

/** The catalogue page of JSONViewer. */
object MonarchJsonViewerSection : CataloguePage {
    override val title = "JSONViewer"

    private const val HEIGHT_STEPS = 5
    private const val SMALL_HEIGHT_STEPS = 3
    private const val ITEMS = 250

    private val payload = MonarchJsonValue.parse(
        """
        {
          "id": "evt_1",
          "type": "invoice.paid",
          "amount": 4200,
          "price": 1.50,
          "paid": true,
          "refund": null,
          "lines": [{"sku": "A1", "qty": 2}, {"sku": "B7", "qty": 1}],
          "customer name": "Ada Lovelace"
        }
        """,
    )

    @Composable
    private fun Chevron() {
        MonarchIcon(
            imageVector = SampleIcons.ChevronRight,
            contentDescription = null,
            size = MonarchIconSize.Sm,
        )
    }

    @Composable
    override fun Content() {
        var selected by rememberSaveable { mutableStateOf("$") }
        CatalogueCaption("A webhook payload")
        MonarchJsonViewer(
            value = payload,
            expandIcon = { Chevron() },
            modifier = Modifier.height(MonarchTheme.spacing.space24 * HEIGHT_STEPS),
            expandDepth = 2,
            searchIcon = {
                MonarchIcon(imageVector = SampleIcons.Search, contentDescription = null)
            },
            onSelect = { path, _ -> selected = path },
        )
        MonarchText(text = "Last selected: $selected", style = MonarchTextStyle.Caption)
        CatalogueCaption("$ITEMS items, 100 at a time")
        MonarchJsonViewer(
            value = MonarchJsonValue.from((1..ITEMS).map { mapOf("n" to it) }),
            expandIcon = { Chevron() },
            modifier = Modifier.height(MonarchTheme.spacing.space24 * SMALL_HEIGHT_STEPS),
            searchable = false,
            rootPath = "",
        )
    }
}
