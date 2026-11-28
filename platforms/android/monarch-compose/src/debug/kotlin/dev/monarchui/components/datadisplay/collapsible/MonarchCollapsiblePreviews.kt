package dev.monarchui.components.datadisplay.collapsible

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.foundation.icon.MonarchIconSize
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.internal.preview.PreviewIcon

@MonarchPreviews
@Composable
internal fun MonarchCollapsiblePreview() {
    PreviewFrame {
        MonarchCollapsible(expanded = false, onExpandedChange = {}, label = "Advanced settings") {
            Text("Hidden")
        }
        MonarchCollapsible(
            expanded = true,
            onExpandedChange = {},
            label = "Details",
            trailingIcon = {
                MonarchIcon(imageVector = PreviewIcon, contentDescription = null, size = MonarchIconSize.Sm)
            },
        ) {
            Text("The invoice was sent on 3 March and paid on 10 March.")
        }
        MonarchCollapsible(expanded = false, onExpandedChange = {}, label = "Managed by your admin", enabled = false) {
            Text("Hidden")
        }
    }
}
