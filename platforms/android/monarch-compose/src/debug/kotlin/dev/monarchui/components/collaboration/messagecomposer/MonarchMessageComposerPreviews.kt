package dev.monarchui.components.collaboration.messagecomposer

import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import dev.monarchui.components.feedback.savestatusindicator.MonarchSaveStatus
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchMessageComposerPreview() {
    PreviewFrame {
        MonarchMessageComposer(
            label = "Message #design",
            state = rememberTextFieldState("See you at 3"),
            onSend = {},
            replyTo = MonarchMessageComposerReply("Ana Ruiz", "Are we still on for the review?"),
            draftStatus = MonarchSaveStatus.Saved,
            onAttach = {},
            onDismissReply = {},
        )
    }
}

@MonarchPreviews
@Composable
internal fun MonarchMessageComposerModesPreview() {
    PreviewFrame {
        MonarchMessageComposer(
            label = "Reply to customer",
            state = rememberTextFieldState(),
            onSend = {},
            placeholder = "Write a reply",
            modes = listOf(
                MonarchMessageComposerMode("reply", "Reply"),
                MonarchMessageComposerMode("note", "Internal note", note = true, sendLabel = "Add note"),
            ),
            selectedMode = "note",
            maxLength = 280,
        )
    }
}
