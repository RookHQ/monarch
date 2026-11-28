package dev.monarchui.components.collaboration.typingindicator

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

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
