package com.rookhq.monarch.components.intelligence.aierrormessage

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchAiErrorMessagePreview() {
    PreviewFrame {
        MonarchAiErrorMessage(kind = MonarchAiErrorKind.Network, announce = false, onRetry = {}, onEditPrompt = {})
        MonarchAiErrorMessage(
            kind = MonarchAiErrorKind.ContextOverflow,
            announce = false,
            onNewConversation = {},
            onSummarize = {},
        )
        MonarchAiErrorMessage(
            kind = MonarchAiErrorKind.RateLimit,
            details = "request_id: req_8f3a2c",
            retryAfterMillis = 12_000L,
            announce = false,
            onRetry = {},
        )
    }
}
