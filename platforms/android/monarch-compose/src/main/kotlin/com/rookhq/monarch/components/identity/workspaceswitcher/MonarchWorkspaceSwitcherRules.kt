package com.rookhq.monarch.components.identity.workspaceswitcher

import com.rookhq.monarch.components.identity.spaceswitcher.MonarchSpaceSwitcherRules

/** The presentation, search and naming logic of MonarchWorkspaceSwitcher, as pure functions. */
internal object MonarchWorkspaceSwitcherRules {
    /** The most workspaces shown in a menu; more, or loading, opens the searchable sheet. */
    const val MENU_LIMIT: Int = 5

    /** Whether the switcher opens a menu (few loaded workspaces) rather than the sheet. */
    fun usesMenu(count: Int, loading: Boolean): Boolean = !loading && count <= MENU_LIMIT

    /** Whether [workspace] matches [query] by name, handle or keywords, ignoring case and accents. */
    fun matches(workspace: MonarchWorkspaceOption, query: String): Boolean =
        MonarchSpaceSwitcherRules.matches(query, workspace.name, workspace.handle, workspace.keywords)

    /** The handle as shown, `@acme`, or `null`. */
    fun handle(workspace: MonarchWorkspaceOption): String? =
        workspace.handle?.takeIf { it.isNotBlank() }?.let { "@$it" }

    /** What TalkBack reads for a workspace: the name and the handle, "Acme, @acme". */
    fun spoken(workspace: MonarchWorkspaceOption): String =
        handle(workspace)?.let { "${workspace.name}, $it" } ?: workspace.name

    /** The current workspace, or `null` when [selection] is `null` or not in [workspaces]. */
    fun current(workspaces: List<MonarchWorkspaceOption>, selection: String?): MonarchWorkspaceOption? =
        selection?.let { id -> workspaces.firstOrNull { it.id == id } }
}
