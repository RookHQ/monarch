package dev.monarchui.sample.sections.files

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.painter.ColorPainter
import dev.monarchui.components.files.filepreview.MonarchFilePreview
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

/** The catalogue page of FilePreview. */
object MonarchFilePreviewSection : CataloguePage {
    override val title = "FilePreview"

    @Composable
    override fun Content() {
        var downloads by rememberSaveable { mutableIntStateOf(0) }
        CatalogueCaption("Text")
        MonarchFilePreview(
            name = "release-notes.md",
            text = "# Release notes\n\n- Faster search\n- Dark mode for charts",
            onDownload = { downloads++ },
            alwaysShowDownload = true,
        )
        CatalogueCaption("Image")
        MonarchFilePreview(name = "diagram.png", image = ColorPainter(MonarchTheme.colors.data3))
        CatalogueCaption("No preview for the type")
        MonarchFilePreview(name = "backup.zip", onDownload = { downloads++ })
        CatalogueCaption("Failed")
        MonarchFilePreview(name = "photo.heic", failed = true, onDownload = { downloads++ })
        MonarchText("Downloads requested: $downloads")
    }
}
