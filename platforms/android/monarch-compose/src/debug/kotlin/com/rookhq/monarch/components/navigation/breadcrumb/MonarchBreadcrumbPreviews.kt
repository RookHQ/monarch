package com.rookhq.monarch.components.navigation.breadcrumb

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchBreadcrumbPreview() {
    PreviewFrame {
        MonarchBreadcrumb(
            items = listOf(MonarchBreadcrumbItem("Projects"), MonarchBreadcrumbItem("Monarch")),
            onItemClick = {},
        )
        MonarchBreadcrumb(
            items = listOf("Drive", "Clients", "Acme", "2026", "Contracts", "Signed").map { MonarchBreadcrumbItem(it) },
            onItemClick = {},
        )
    }
}
