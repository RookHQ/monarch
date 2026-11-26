package com.rookhq.monarch.sample.sections.datadisplay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import com.rookhq.monarch.components.datadisplay.favicon.MonarchFavicon
import com.rookhq.monarch.components.datadisplay.favicon.MonarchFaviconShape
import com.rookhq.monarch.components.datadisplay.favicon.MonarchFaviconSize
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.sampleicons.SampleIcons
import com.rookhq.monarch.theme.MonarchTheme

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
