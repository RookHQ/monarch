package com.rookhq.monarch.components.collaboration.typingindicator

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchTypingIndicatorPreview() {
    PreviewFrame {
        MonarchTypingIndicator(names = listOf("Ana"))
        MonarchTypingIndicator(names = listOf("Ana", "Ben"))
        MonarchTypingIndicator(names = listOf("Ana", "Ben", "Chen"))
        MonarchTypingIndicator(names = emptyList())
    }
}
