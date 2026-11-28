package dev.monarchui.sample.sections.files

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.painter.ColorPainter
import dev.monarchui.components.files.videothumbnail.MonarchVideoThumbnail
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

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
