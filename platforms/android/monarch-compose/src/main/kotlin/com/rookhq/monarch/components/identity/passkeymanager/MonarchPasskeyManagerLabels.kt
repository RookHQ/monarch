package com.rookhq.monarch.components.identity.passkeymanager

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchPasskeyManager`. Every
 * `null` keeps the built-in string `monarch_passkeymanager_<key>`.
 *
 * @property listLabel the spoken name of the list, "Your passkeys".
 * @property add the add button, "Add a passkey".
 * @property rename the row's rename button, "Rename".
 * @property renameLabel the spoken name of a rename button: "Rename Pixel".
 * @property renameTitle the title of the rename dialog, "Rename passkey".
 * @property nameField the label of the name field, "Name".
 * @property save the button that saves a new name, "Save".
 * @property remove the row's remove button, "Remove".
 * @property removeLabel the spoken name of a remove button: "Remove Pixel".
 * @property created before the time it was added, "Added".
 * @property lastUsed before the time it was last used, "Last used".
 * @property neverUsed instead of the last-used time when there is none, "Never used".
 * @property synced the badge of a synced passkey, "Synced".
 * @property confirmTitle the title of the removal confirmation: "Remove Pixel?".
 * @property confirmMessage the text of the removal confirmation.
 * @property lastMethodWarning the warning when the last passkey is the only way to sign in.
 * @property confirmRemove the confirm button of the removal, "Remove passkey".
 * @property cancel the button that closes a dialog, "Cancel".
 * @property unsupported the warning when the device cannot create passkeys.
 * @property cancelled the note when the person cancelled adding a passkey.
 * @property addFailed the failure when adding a passkey failed.
 * @property added announced after a passkey is added, "Passkey added".
 * @property renamed announced after a passkey is renamed: "Renamed to Work laptop".
 * @property removed announced after a passkey is removed: "Removed Pixel".
 * @property renameFailed the failure when renaming failed.
 * @property removeFailed the failure when removing failed: "Couldn't remove Pixel. Try again.".
 * @property loading the loading state, "Loading passkeys".
 * @property emptyTitle the empty state, "No passkeys yet".
 * @property emptyMessage the line under the empty state.
 * @property errorTitle the failed state, "Couldn't load your passkeys".
 */
@Immutable
public class MonarchPasskeyManagerLabels(
    public val listLabel: String? = null,
    public val add: String? = null,
    public val rename: String? = null,
    public val renameLabel: ((String) -> String)? = null,
    public val renameTitle: String? = null,
    public val nameField: String? = null,
    public val save: String? = null,
    public val remove: String? = null,
    public val removeLabel: ((String) -> String)? = null,
    public val created: String? = null,
    public val lastUsed: String? = null,
    public val neverUsed: String? = null,
    public val synced: String? = null,
    public val confirmTitle: ((String) -> String)? = null,
    public val confirmMessage: String? = null,
    public val lastMethodWarning: String? = null,
    public val confirmRemove: String? = null,
    public val cancel: String? = null,
    public val unsupported: String? = null,
    public val cancelled: String? = null,
    public val addFailed: String? = null,
    public val added: String? = null,
    public val renamed: ((String) -> String)? = null,
    public val removed: ((String) -> String)? = null,
    public val renameFailed: String? = null,
    public val removeFailed: ((String) -> String)? = null,
    public val loading: String? = null,
    public val emptyTitle: String? = null,
    public val emptyMessage: String? = null,
    public val errorTitle: String? = null,
)
