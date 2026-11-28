package com.rookhq.monarch.components.collaboration.chatdock

import androidx.compose.runtime.Composable
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchChatDockPreview() {
    PreviewFrame {
        MonarchChatDock(open = false, onOpenChange = {}, unread = 3) { MonarchText(text = "Chat") }
        MonarchChatDock(open = false, onOpenChange = {}) { MonarchText(text = "Chat") }
    }
}
