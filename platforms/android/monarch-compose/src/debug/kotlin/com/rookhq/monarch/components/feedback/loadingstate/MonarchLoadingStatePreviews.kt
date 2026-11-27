package com.rookhq.monarch.components.feedback.loadingstate

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchLoadingStatePreview() {
    PreviewFrame {
        MonarchLoadingState(label = "Loading tickets", rows = 2)
        MonarchLoadingState(label = "Loading settings", variant = MonarchLoadingStateVariant.Rows, rows = 2)
        MonarchLoadingState(label = "Loading projects", variant = MonarchLoadingStateVariant.Cards, rows = 1)
        MonarchLoadingState(label = "Loading the invoice", variant = MonarchLoadingStateVariant.Detail)
        MonarchLoadingState(label = "Loading reports", variant = MonarchLoadingStateVariant.Spinner)
    }
}
