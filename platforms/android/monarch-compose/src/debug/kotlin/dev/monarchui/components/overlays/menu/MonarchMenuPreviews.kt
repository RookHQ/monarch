package dev.monarchui.components.overlays.menu

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.internal.preview.PreviewIcon

@MonarchPreviews
@Composable
internal fun MonarchMenuPreview() {
    PreviewFrame {
        Box {
            MonarchButton(text = "Actions", onClick = {})
            MonarchMenu(expanded = true, onDismissRequest = {}) {
                MonarchMenuItem(
                    text = "Rename",
                    onClick = {},
                    leadingIcon = { MonarchIcon(imageVector = PreviewIcon, contentDescription = null) },
                )
                MonarchMenuItem(text = "Duplicate", onClick = {}, shortcut = "Ctrl+D")
                MonarchMenuDivider()
                MonarchMenuLabel(text = "Sort by")
                MonarchMenuItem(text = "Newest", onClick = {}, selected = true)
                MonarchMenuItem(text = "Oldest", onClick = {}, selected = false)
                MonarchMenuDivider()
                MonarchMenuItem(text = "Delete", onClick = {}, destructive = true)
            }
        }
    }
}
