package dev.monarchui.components.identity.presencedot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.theme.MonarchTheme

@MonarchPreviews
@Composable
internal fun MonarchPresenceDotPreview() {
    PreviewFrame {
        MonarchPresenceDotSize.entries.forEach { size ->
            Row(horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2)) {
                MonarchPresenceStatus.entries.forEach { status -> MonarchPresenceDot(status = status, size = size) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2)) {
            MonarchPresenceStatus.entries.forEach { status ->
                MonarchPresenceDot(status = status, size = MonarchPresenceDotSize.Lg, overlay = true)
            }
        }
    }
}
