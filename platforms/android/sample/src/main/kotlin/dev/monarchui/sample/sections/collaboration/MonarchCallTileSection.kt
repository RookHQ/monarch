package dev.monarchui.sample.sections.collaboration

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.collaboration.calltile.MonarchCallQuality
import dev.monarchui.components.collaboration.calltile.MonarchCallReaction
import dev.monarchui.components.collaboration.calltile.MonarchCallTile
import dev.monarchui.components.collaboration.calltile.MonarchCallTileSize
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

/** The catalogue page of CallTile. */
object MonarchCallTileSection : CataloguePage {
    override val title = "CallTile"

    /** Stands in for the call SDK's video renderer. */
    @Composable
    private fun SampleVideo() {
        Box(
            modifier = Modifier.fillMaxSize().background(MonarchTheme.colors.bgSubtle),
            contentAlignment = Alignment.Center,
        ) { MonarchText(text = "Camera") }
    }

    @Composable
    override fun Content() {
        var pinned by rememberSaveable { mutableStateOf(false) }
        var muted by rememberSaveable { mutableStateOf(false) }
        var hand by rememberSaveable { mutableStateOf(false) }
        var reactionAt by rememberSaveable { mutableLongStateOf(0L) }
        var fullScreen by rememberSaveable { mutableStateOf(false) }
        CatalogueCaption("Grid tile with Pin and full screen")
        MonarchCallTile(
            name = "Ana",
            modifier = Modifier.fillMaxWidth(),
            speaking = !muted,
            micOff = muted,
            handRaised = hand,
            pinned = pinned,
            quality = MonarchCallQuality.Good,
            reaction = if (reactionAt > 0L) MonarchCallReaction("👏", reactionAt) else null,
            onPinnedChange = { pinned = it },
            onFullScreen = { fullScreen = !fullScreen },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2)) {
            MonarchButton(text = if (muted) "Unmute" else "Mute", onClick = { muted = !muted })
            MonarchButton(text = "Hand", onClick = { hand = !hand })
            MonarchButton(text = "React", onClick = { reactionAt = System.currentTimeMillis() })
        }
        if (fullScreen) MonarchText(text = "The app would show Ana full screen.")
        CatalogueCaption("Your own video, reconnecting")
        MonarchCallTile(
            name = "Ben",
            modifier = Modifier.fillMaxWidth(),
            self = true,
            status = "Reconnecting…",
            statusTone = MonarchTone.Warning,
            quality = MonarchCallQuality.Poor,
        ) { SampleVideo() }
        CatalogueCaption("Filmstrip")
        Row(horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2)) {
            MonarchCallTile(
                name = "Chloé",
                modifier = Modifier.weight(1f),
                size = MonarchCallTileSize.Sm,
                screen = true,
            )
            MonarchCallTile(
                name = "Dev",
                modifier = Modifier.weight(1f),
                size = MonarchCallTileSize.Sm,
                micOff = true,
            )
        }
    }
}
