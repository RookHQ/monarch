package dev.monarchui.components.collaboration.chatdock

import androidx.compose.runtime.Composable
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchChatDockPreview() {
    PreviewFrame {
        MonarchChatDock(open = false, onOpenChange = {}, unread = 3) { MonarchText(text = "Chat") }
        MonarchChatDock(open = false, onOpenChange = {}) { MonarchText(text = "Chat") }
    }
}
