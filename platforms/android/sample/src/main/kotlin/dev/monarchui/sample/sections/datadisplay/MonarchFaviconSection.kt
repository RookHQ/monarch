package dev.monarchui.sample.sections.datadisplay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import dev.monarchui.components.datadisplay.favicon.MonarchFavicon
import dev.monarchui.components.datadisplay.favicon.MonarchFaviconShape
import dev.monarchui.components.datadisplay.favicon.MonarchFaviconSize
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sampleicons.SampleIcons
import dev.monarchui.theme.MonarchTheme

/** The catalogue page of Favicon. */
object MonarchFaviconSection : CataloguePage {
    override val title = "Favicon"

    @Composable
    override fun Content() {
        MonarchFaviconShape.entries.forEach { shape ->
            CatalogueCaption(shape.name)
            Row(
                horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space3),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MonarchFaviconSize.entries.forEach { size ->
                    MonarchFavicon(image = null, name = "example.com", size = size, shape = shape)
                    val icon = rememberVectorPainter(SampleIcons.Search)
                    MonarchFavicon(image = icon, name = "plus", size = size, shape = shape)
                }
            }
        }
        CatalogueCaption("Beside the site name")
        Row(
            horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MonarchFavicon(image = null, name = "docs.example.com")
            MonarchText(text = "docs.example.com")
        }
    }
}
