package com.rookhq.monarch.components.typography.kbd

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.theme.MonarchTheme

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
