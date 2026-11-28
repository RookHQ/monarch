package dev.monarchui.components.overlays.lightbox

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.unit.dp
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.theme.MonarchTheme

@MonarchPreviews
@Composable
internal fun MonarchLightboxPreview() {
    PreviewFrame {
        MonarchLightboxContent(
            items = listOf(
                MonarchLightboxItem(
                    image = ColorPainter(MonarchTheme.colors.bgMuted),
                    alt = "Whiteboard sketch of the checkout flow",
                    caption = "Sketch from the planning day",
                ),
                MonarchLightboxItem(image = null, alt = "Bar chart of weekly sign-ups"),
            ),
            selectedIndex = 0,
            onSelectedIndexChange = {},
            onDismissRequest = {},
            modifier = Modifier.height(480.dp),
            looping = false,
            maximumZoom = 4f,
            onShare = {},
            labels = MonarchLightboxLabels(),
        )
    }
}

@MonarchPreviews
@Composable
internal fun MonarchLightboxUnavailablePreview() {
    PreviewFrame {
        MonarchLightboxContent(
            items = listOf(MonarchLightboxItem(image = null, alt = "Team photo")),
            selectedIndex = 0,
            onSelectedIndexChange = {},
            onDismissRequest = {},
            modifier = Modifier.height(320.dp),
            looping = false,
            maximumZoom = 4f,
            onShare = null,
            labels = MonarchLightboxLabels(),
        )
    }
}
