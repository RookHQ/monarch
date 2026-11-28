package dev.monarchui.components.feedback.emptystate

import androidx.compose.runtime.Composable
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.internal.preview.PreviewIcon

@MonarchPreviews
@Composable
internal fun MonarchEmptyStateKindsPreview() {
    PreviewFrame {
        MonarchEmptyState(
            title = "No invoices yet",
            message = "Invoices you create appear here.",
            icon = { MonarchIcon(PreviewIcon, contentDescription = null) },
            actions = {
                MonarchButton(text = "Create invoice", onClick = {}, variant = MonarchButtonVariant.Primary)
            },
        )
        MonarchEmptyState(
            title = "No matching tickets",
            kind = MonarchEmptyStateKind.Filtered,
            size = MonarchEmptyStateSize.Sm,
            onClearFilters = {},
        )
        MonarchEmptyState(
            title = "All caught up",
            kind = MonarchEmptyStateKind.Success,
            icon = { MonarchIcon(PreviewIcon, contentDescription = null) },
            tip = "New tickets appear here as they arrive.",
        )
    }
}
