package dev.monarchui.components.technical.scopeselector

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchScopeSelectorPreview() {
    PreviewFrame {
        MonarchScopeSelector(
            groups = listOf(
                MonarchScopeGroup(
                    id = "projects",
                    label = "Projects API",
                    description = "Owned by Platform",
                    scopes = listOf(
                        MonarchScopeOption("projects:read", "Read projects"),
                        MonarchScopeOption("projects:write", "Change projects"),
                    ),
                ),
                MonarchScopeGroup(id = "billing", label = "Billing API", scopes = emptyList(), loading = true),
            ),
            value = listOf("projects:read"),
            onValueChange = {},
            defaultExpanded = setOf("projects"),
        )
    }
}
