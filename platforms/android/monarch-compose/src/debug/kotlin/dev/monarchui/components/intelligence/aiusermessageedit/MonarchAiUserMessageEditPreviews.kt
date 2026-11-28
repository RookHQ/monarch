package dev.monarchui.components.intelligence.aiusermessageedit

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchAiUserMessageEditPreview() {
    PreviewFrame {
        MonarchAiUserMessageEdit(
            original = "Summarise the quarterly report.",
            onSubmit = {},
            onCancel = {},
            focusOnAppear = false,
        )
        MonarchAiUserMessageEdit(original = "Draft a reply.", onSubmit = {}, onCancel = {}, enabled = false)
    }
}
