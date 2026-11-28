package com.rookhq.monarch.components.feedback.emptystate

import androidx.compose.runtime.Composable
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.internal.preview.PreviewIcon

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
