package dev.monarchui.sample.sections.collaboration

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.painter.ColorPainter
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.collaboration.linkcard.MonarchLinkCard
import dev.monarchui.components.collaboration.linkcard.MonarchLinkCardSize
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sampleicons.SampleIcons
import dev.monarchui.theme.MonarchTheme

/** The catalogue page of LinkCard. */
object MonarchLinkCardSection : CataloguePage {
    override val title = "LinkCard"

    @Composable
    override fun Content() {
        var shown by rememberSaveable { mutableStateOf(true) }
        CatalogueCaption("Banner, removable")
        if (shown) {
            MonarchLinkCard(
                title = "Release notes for March",
                url = "https://www.example.com/blog/march",
                description = "Faster search, shared views and a new API.",
                image = ColorPainter(MonarchTheme.colors.data1),
                imageAspectRatio = 1.9f,
                onDismiss = { shown = false },
                dismissIcon = { MonarchIcon(imageVector = SampleIcons.Close, contentDescription = null) },
            )
        } else {
            MonarchButton(text = "Show the preview again", onClick = { shown = true })
        }
        CatalogueCaption("Thumbnail, video, picture loading")
        MonarchLinkCard(
            title = "Product tour",
            url = "https://example.com/tour",
            description = "Four minutes on boards, views and sharing.",
            hasImage = true,
            video = true,
            size = MonarchLinkCardSize.Sm,
        )
        CatalogueCaption("No picture, not a safe link")
        MonarchLinkCard(title = "Internal wiki", url = "intranet://wiki/home", siteName = "Wiki")
    }
}
