package com.rookhq.monarch.sample.sections.files

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.painter.ColorPainter
import com.rookhq.monarch.components.files.videothumbnail.MonarchVideoThumbnail
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

/** The catalogue page of VideoThumbnail. */
object MonarchVideoThumbnailSection : CataloguePage {
    override val title = "VideoThumbnail"

    @Composable
    override fun Content() {
        var plays by rememberSaveable { mutableIntStateOf(0) }
        val poster = ColorPainter(MonarchTheme.colors.data1)
        CatalogueCaption("A button")
        MonarchVideoThumbnail(title = "Product tour", image = poster, onPlay = { plays++ }, durationMillis = 65_000L)
        MonarchText("Played $plays times")
        CatalogueCaption("Disabled")
        MonarchVideoThumbnail(title = "Product tour", image = poster, onPlay = {}, enabled = false)
        CatalogueCaption("No poster yet")
        MonarchVideoThumbnail(title = "Webinar", image = null, durationMillis = 3_725_000L)
    }
}
