package dev.monarchui.components.typography.kbd

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.theme.MonarchTheme

@MonarchPreviews
@Composable
internal fun MonarchKbdPreview() {
    PreviewFrame {
        Row(horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2)) {
            MonarchKbd(keys = "⌘K", alwaysVisible = true)
            MonarchKbd(keys = "⇧⌘P", alwaysVisible = true)
            MonarchKbd(keys = "Esc", alwaysVisible = true)
            MonarchKbd(keys = "/", alwaysVisible = true)
        }
    }
}
