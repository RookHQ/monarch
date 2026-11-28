package dev.monarchui.components.navigation.breadcrumb

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

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
