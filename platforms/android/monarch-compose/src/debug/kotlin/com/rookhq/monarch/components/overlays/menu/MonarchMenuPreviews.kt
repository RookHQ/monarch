package com.rookhq.monarch.components.overlays.menu

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.internal.preview.PreviewIcon

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
