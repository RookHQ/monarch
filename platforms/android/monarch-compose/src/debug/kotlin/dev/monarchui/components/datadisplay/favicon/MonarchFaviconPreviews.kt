package dev.monarchui.components.datadisplay.favicon

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.internal.preview.PreviewIcon
import dev.monarchui.theme.MonarchTheme

@MonarchPreviews
@Composable
internal fun MonarchFaviconPreview() {
    PreviewFrame {
        MonarchFaviconShape.entries.forEach { shape ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MonarchFaviconSize.entries.forEach { size ->
                    MonarchFavicon(image = null, name = "example.com", size = size, shape = shape)
                    val icon = rememberVectorPainter(PreviewIcon)
                    MonarchFavicon(image = icon, name = "rook", size = size, shape = shape)
                }
            }
        }
    }
}
