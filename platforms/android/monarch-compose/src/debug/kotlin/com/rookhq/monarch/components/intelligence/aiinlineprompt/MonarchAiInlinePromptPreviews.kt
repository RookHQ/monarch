package com.rookhq.monarch.components.intelligence.aiinlineprompt

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

private const val PreviewSelection =
    "The team shipped the new onboarding flow in March. Early numbers suggest more people finish setup " +
        "on their first visit, although the sample is still small and support tickets have not changed much yet."

/** The content of the sheet, with an excerpt and suggestions. The sheet itself opens in a window of its own. */
@MonarchPreviews
@Composable
internal fun MonarchAiInlinePromptPreview() {
    PreviewFrame {
        AiInlinePromptPanel(
            onSend = {},
            context = PreviewSelection,
            suggestions = listOf("Shorten", "Make it more formal", "Fix spelling and grammar"),
        )
    }
}

/** Sending: the field and the suggestions are disabled and the status is shown. */
@MonarchPreviews
@Composable
internal fun MonarchAiInlinePromptPendingPreview() {
    PreviewFrame {
        AiInlinePromptPanel(
            onSend = {},
            context = "Early numbers suggest more people finish setup.",
            suggestions = listOf("Shorten"),
            initialValue = "Make it shorter",
            pending = true,
        )
    }
}
