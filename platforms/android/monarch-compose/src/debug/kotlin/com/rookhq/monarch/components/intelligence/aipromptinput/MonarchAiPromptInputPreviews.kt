package com.rookhq.monarch.components.intelligence.aipromptinput

import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import com.rookhq.monarch.components.intelligence.aiattachmenttray.MonarchAiAttachmentTrayFile
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchAiPromptInputPreview() {
    PreviewFrame {
        MonarchAiPromptInput(
            label = "Message the assistant",
            state = rememberTextFieldState(),
            onSubmit = {},
            onAttach = {},
        )
        MonarchAiPromptInput(
            label = "Message the assistant",
            state = rememberTextFieldState("Summarize the last three tickets"),
            onSubmit = {},
            maxLength = 40,
            countTokens = { it.length / 4 },
            attachments = listOf(MonarchAiAttachmentTrayFile("f1", "brief.pdf", sizeBytes = 120_000)),
            onRemoveAttachment = {},
        )
        MonarchAiPromptInput(
            label = "Prompt",
            state = rememberTextFieldState(),
            onSubmit = {},
            busy = true,
            onStop = {},
        )
        MonarchAiPromptInput(
            label = "Prompt",
            state = rememberTextFieldState(),
            onSubmit = {},
            enabled = false,
            disabledReason = "Connect a provider to start.",
        )
    }
}
