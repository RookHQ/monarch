package com.rookhq.monarch.components.technical.scopeselector

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

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
