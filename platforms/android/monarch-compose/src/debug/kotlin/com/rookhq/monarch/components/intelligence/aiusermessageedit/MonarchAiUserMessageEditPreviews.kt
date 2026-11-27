package com.rookhq.monarch.components.intelligence.aiusermessageedit

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

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
