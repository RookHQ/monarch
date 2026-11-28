package com.rookhq.monarch.sample.sections.technical

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.rookhq.monarch.components.technical.jsonviewer.MonarchJsonValue
import com.rookhq.monarch.components.technical.jsonviewer.MonarchJsonViewer
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.foundation.icon.MonarchIconSize
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.sampleicons.SampleIcons
import com.rookhq.monarch.theme.MonarchTheme

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
