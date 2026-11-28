package dev.monarchui.components.identity.workspaceswitcher

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchWorkspaceSwitcher`.
 * Every `null` keeps the built-in string `monarch_workspaceswitcher_<key>`.
 *
 * @property label the name of the button and the title of the sheet, "Workspace".
 * @property placeholder the button text while no workspace is chosen, "Choose a workspace".
 * @property search the placeholder of the search field, "Search workspaces".
 * @property empty shown when nothing matches the search, "No workspaces match.".
 * @property loading shown while the workspaces load, "Loading workspaces…".
 * @property create the Create workspace action.
 * @property manage the All workspaces action.
 * @property cancel the button that closes the sheet without a change.
 */
@Immutable
public class MonarchWorkspaceSwitcherLabels(
    public val label: String? = null,
    public val placeholder: String? = null,
    public val search: String? = null,
    public val empty: String? = null,
    public val loading: String? = null,
    public val create: String? = null,
    public val manage: String? = null,
    public val cancel: String? = null,
)
