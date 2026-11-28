package dev.monarchui.sample.sections.collaboration

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.monarchui.components.collaboration.screenshareview.MonarchScreenShareView
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

/** The catalogue page of ScreenShareView. */
object MonarchScreenShareViewSection : CataloguePage {
    override val title = "ScreenShareView"

    /** Stands in for the call SDK's video view. */
    @Composable
    private fun SampleScreen() {
        Box(
            modifier = Modifier.fillMaxSize().background(MonarchTheme.colors.bgSubtle),
            contentAlignment = Alignment.Center,
        ) {
            MonarchText(text = "Quarterly report, slide 4 of 12")
        }
    }

    @Composable
    override fun Content() {
        var zoom by rememberSaveable { mutableFloatStateOf(1f) }
        var pictureInPicture by rememberSaveable { mutableStateOf(false) }
        var sharing by rememberSaveable { mutableStateOf(true) }
        CatalogueCaption("Someone else is presenting")
        MonarchScreenShareView(
            presenterName = "Ana",
            zoom = zoom,
            onZoomChange = { zoom = it },
            pictureInPictureActive = pictureInPicture,
            onPictureInPicture = { pictureInPicture = !pictureInPicture },
        ) { SampleScreen() }
        CatalogueCaption("You are presenting")
        MonarchScreenShareView(
            presenterName = "You",
            local = true,
            waiting = !sharing,
            onStopSharing = { sharing = false },
        ) { SampleScreen() }
        CatalogueCaption("Waiting")
        MonarchScreenShareView(presenterName = "Ben", waiting = true) { SampleScreen() }
    }
}
