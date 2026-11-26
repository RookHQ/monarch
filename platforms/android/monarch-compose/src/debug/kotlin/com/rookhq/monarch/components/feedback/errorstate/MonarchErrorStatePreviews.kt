package com.rookhq.monarch.components.feedback.errorstate

import androidx.compose.runtime.Composable
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.internal.preview.PreviewIcon

@MonarchPreviews
@Composable
internal fun MonarchErrorStatePreview() {
    PreviewFrame {
        MonarchErrorState(
            title = "Invoices did not load",
            message = "The billing service did not answer. Try again in a moment.",
            icon = { MonarchIcon(PreviewIcon, contentDescription = null) },
            requestId = "req_8f2a91",
            onRetry = {},
        )
        MonarchErrorState(
            title = "You do not have access",
            kind = MonarchErrorStateKind.Forbidden,
            icon = { MonarchIcon(PreviewIcon, contentDescription = null) },
            onRetry = {},
        )
    }
}
