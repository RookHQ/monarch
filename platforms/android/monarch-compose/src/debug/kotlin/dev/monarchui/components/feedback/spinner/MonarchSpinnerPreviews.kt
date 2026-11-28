package dev.monarchui.components.feedback.spinner

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.theme.MonarchTheme

@MonarchPreviews
@Composable
internal fun MonarchSpinnerPreview() {
    PreviewFrame {
        MonarchSpinnerSize.entries.forEach { size ->
            MonarchSpinner(size = size, color = MonarchTheme.colors.accentStrong)
        }
    }
}
