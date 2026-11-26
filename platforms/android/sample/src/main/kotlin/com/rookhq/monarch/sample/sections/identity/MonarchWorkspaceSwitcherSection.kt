package com.rookhq.monarch.sample.sections.identity

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.identity.workspaceswitcher.MonarchWorkspaceOption
import com.rookhq.monarch.components.identity.workspaceswitcher.MonarchWorkspaceSwitcher
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

private const val ClientCount = 8

/** The catalogue page of WorkspaceSwitcher. */
object MonarchWorkspaceSwitcherSection : CataloguePage {
    override val title = "WorkspaceSwitcher"

    @Composable
    override fun Content() {
        var workspace by rememberSaveable { mutableStateOf<String?>("acme") }
        var client by rememberSaveable { mutableStateOf<String?>("c1") }
        CatalogueCaption("A few workspaces: a menu")
        MonarchWorkspaceSwitcher(
            workspaces = listOf(
                MonarchWorkspaceOption("acme", "Acme", handle = "acme"),
                MonarchWorkspaceOption("rook", "Rook Labs", handle = "rooklabs", keywords = "Rook Studio"),
            ),
            selection = workspace,
            onSelectionChange = { workspace = it },
            onCreate = {},
            onManage = {},
        )
        CatalogueCaption("Many workspaces: a searchable sheet")
        MonarchWorkspaceSwitcher(
            workspaces = (1..ClientCount).map { MonarchWorkspaceOption("c$it", "Client $it", handle = "client$it") },
            selection = client,
            onSelectionChange = { client = it },
            size = MonarchButtonSize.Sm,
            onCreate = {},
        )
        CatalogueCaption("Loading, icon only")
        MonarchWorkspaceSwitcher(
            workspaces = emptyList(),
            selection = null,
            onSelectionChange = {},
            loading = true,
            iconOnly = true,
        )
    }
}
