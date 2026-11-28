package com.rookhq.monarch.components.feedback.spinner

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.theme.MonarchTheme

@MonarchPreviews
@Composable
internal fun MonarchSpinnerPreview() {
    PreviewFrame {
        MonarchSpinnerSize.entries.forEach { size ->
            MonarchSpinner(size = size, color = MonarchTheme.colors.accentStrong)
        }
    }
}
