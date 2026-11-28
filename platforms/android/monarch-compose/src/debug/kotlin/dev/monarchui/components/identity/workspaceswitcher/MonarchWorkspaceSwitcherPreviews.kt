package dev.monarchui.components.identity.workspaceswitcher

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

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
