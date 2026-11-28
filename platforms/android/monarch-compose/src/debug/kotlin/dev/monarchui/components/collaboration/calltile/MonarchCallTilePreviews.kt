package dev.monarchui.components.collaboration.calltile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.theme.MonarchTheme

private const val PreviewNow = 1_700_000_000_000L
private const val PreviewReactionAge = 1_000L

@MonarchPreviews
@Composable
internal fun MonarchCallTilePreview() {
    PreviewFrame {
        MonarchCallTile(
            name = "Ana",
            speaking = true,
            handRaised = true,
            quality = MonarchCallQuality.Good,
            onPinnedChange = {},
            onFullScreen = {},
            reaction = MonarchCallReaction("👏", PreviewNow - PreviewReactionAge),
            now = PreviewNow,
        )
        MonarchCallTile(
            name = "Ben",
            self = true,
            micOff = true,
            status = "Reconnecting…",
            statusTone = MonarchTone.Warning,
            quality = MonarchCallQuality.Poor,
        ) { Box(Modifier.fillMaxSize().background(MonarchTheme.colors.bgMuted)) }
        MonarchCallTile(name = "Chloé", size = MonarchCallTileSize.Sm, screen = true)
    }
}
