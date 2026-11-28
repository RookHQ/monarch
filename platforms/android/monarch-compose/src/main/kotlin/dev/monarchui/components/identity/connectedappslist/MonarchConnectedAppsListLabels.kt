package dev.monarchui.components.identity.connectedappslist

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchConnectedAppsList`.
 * Every `null` keeps the built-in string `monarch_connectedappslist_<key>`.
 *
 * @property listLabel the spoken name of the list, "Connected apps".
 * @property verified the badge of a reviewed app, "Verified".
 * @property suspended the badge of a suspended app, "Suspended".
 * @property connected before the connected time, "Connected".
 * @property lastUsed before the last-used time, "Last used".
 * @property workspace before the workspace name, "Workspace".
 * @property scopesLabel the spoken name of an app's scope list: "What Notes can access".
 * @property revoke the row button and the confirm button, "Remove access".
 * @property revokeLabel the spoken name of a row button: "Remove access for Notes".
 * @property confirmTitle the title of the confirmation: "Remove access for Notes?".
 * @property confirmMessage the text of the confirmation.
 * @property cancel the button that closes the confirmation, "Cancel".
 * @property revoked announced after access is removed: "Removed access for Notes".
 * @property revokeFailed shown when removal failed without a message: "Couldn't remove access for Notes. Try again.".
 * @property loading the loading state, "Loading connected apps".
 * @property emptyTitle the empty state, "No connected apps".
 * @property emptyMessage the line under the empty state.
 * @property errorTitle the error state, "Couldn't load your connected apps".
 */
@Immutable
public class MonarchConnectedAppsListLabels(
    public val listLabel: String? = null,
    public val verified: String? = null,
    public val suspended: String? = null,
    public val connected: String? = null,
    public val lastUsed: String? = null,
    public val workspace: String? = null,
    public val scopesLabel: ((String) -> String)? = null,
    public val revoke: String? = null,
    public val revokeLabel: ((String) -> String)? = null,
    public val confirmTitle: ((String) -> String)? = null,
    public val confirmMessage: String? = null,
    public val cancel: String? = null,
    public val revoked: ((String) -> String)? = null,
    public val revokeFailed: ((String) -> String)? = null,
    public val loading: String? = null,
    public val emptyTitle: String? = null,
    public val emptyMessage: String? = null,
    public val errorTitle: String? = null,
)
