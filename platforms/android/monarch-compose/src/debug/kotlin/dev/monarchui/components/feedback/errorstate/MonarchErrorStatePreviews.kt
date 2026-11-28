package dev.monarchui.components.feedback.errorstate

import androidx.compose.runtime.Composable
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.internal.preview.PreviewIcon

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
