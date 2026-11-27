package com.rookhq.monarch.components.identity.workspaceswitcher

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

private val PreviewWorkspaces = listOf(
    MonarchWorkspaceOption("acme", "Acme", handle = "acme"),
    MonarchWorkspaceOption("rook", "Rook Labs", handle = "rooklabs"),
)

@MonarchPreviews
@Composable
internal fun MonarchWorkspaceSwitcherPreview() {
    PreviewFrame {
        MonarchWorkspaceSwitcher(workspaces = PreviewWorkspaces, selection = "acme", onSelectionChange = {})
        MonarchWorkspaceSwitcher(workspaces = PreviewWorkspaces, selection = null, onSelectionChange = {})
        MonarchWorkspaceSwitcher(
            workspaces = PreviewWorkspaces,
            selection = "rook",
            onSelectionChange = {},
            iconOnly = true,
        )
    }
}
