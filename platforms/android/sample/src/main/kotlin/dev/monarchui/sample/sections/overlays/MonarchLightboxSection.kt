package dev.monarchui.sample.sections.overlays

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.overlays.lightbox.MonarchLightbox
import dev.monarchui.components.overlays.lightbox.MonarchLightboxItem
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sample.CataloguePictures
import dev.monarchui.theme.MonarchTheme

/**
 * The catalogue page of Lightbox. The catalogue has no photos of its own, so
 * it shows framework drawables; the third item has no picture, to show the
 * unavailable state.
 */
object MonarchLightboxSection : CataloguePage {
    override val title = "Lightbox"

    @Composable
    override fun Content() {
        var viewing by rememberSaveable { mutableStateOf(false) }
        var index by rememberSaveable { mutableIntStateOf(0) }
        var looping by rememberSaveable { mutableStateOf(false) }
        val items = listOf(
            MonarchLightboxItem(
                image = CataloguePictures.landscape(),
                alt = "Gallery placeholder",
                caption = "Sketch from the planning day",
            ),
            MonarchLightboxItem(
                image = CataloguePictures.landscape(),
                alt = "Map placeholder",
                caption = "The office in Lisbon",
            ),
            MonarchLightboxItem(image = null, alt = "Team photo"),
        )
        CatalogueCaption("Open the viewer")
        MonarchButton(
            text = "View three items",
            onClick = {
                looping = false
                index = 0
                viewing = true
            },
        )
        MonarchButton(
            text = "View with looping",
            onClick = {
                looping = true
                index = 0
                viewing = true
            },
        )
        MonarchText(
            text = "Swipe, pinch or double-tap; or use the buttons. Back closes.",
            style = MonarchTextStyle.Caption,
            color = MonarchTheme.colors.textMuted,
        )
        if (viewing) {
            MonarchLightbox(
                items = items,
                selectedIndex = index,
                onSelectedIndexChange = { index = it },
                onDismissRequest = { viewing = false },
                looping = looping,
            )
        }
    }
}
