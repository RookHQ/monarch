package com.rookhq.monarch.components.technical.apikeyrow

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchApiKeyRow`. Every
 * `null` uses the built-in string in `strings_apikeyrow.xml`. Strings that
 * name something take it as a parameter.
 *
 * @property prefix writes what TalkBack reads for the masked key from its prefix: "Key starting with sk_live_4f2a".
 * @property created the word before the creation time: "Created".
 * @property lastUsed the word before the last use: "Last used".
 * @property neverUsed shown instead of the last use when there is none: "Never used".
 * @property expires the word before the expiry: "Expires".
 * @property scopes the spoken name of the scope list: "Scopes".
 * @property noScopes shown when the key has no scopes: "No scopes".
 * @property moreScopes writes the badge for the scopes past the limit from their number: "+2".
 * @property status writes the badge's text from the status: "Active", "Expiring soon", "Expired", "Revoked".
 * @property menu the visible text of the actions button when no icon is given: "Actions".
 * @property actions writes the spoken name of the actions button from the key's name: "Actions for Reporting job".
 * @property rotate the menu item that rotates the key: "Rotate".
 * @property revoke the menu item that revokes the key: "Revoke".
 * @property rotateTitle writes the rotation dialog's title from the key's name: "Rotate Reporting job?".
 * @property rotateMessage the rotation dialog's message.
 * @property rotateConfirm the rotation dialog's confirm button: "Rotate key".
 * @property revokeTitle writes the revocation dialog's title from the key's name: "Revoke Reporting job?".
 * @property revokeMessage the revocation dialog's message.
 * @property revokeConfirm the revocation dialog's confirm button: "Revoke key".
 * @property revokePhrasePrompt writes the label of the field where the phrase is typed from the phrase:
 * "Type Reporting job to confirm".
 * @property cancel the dialogs' cancel button: "Cancel".
 * @property rotating shown and announced while the rotation runs: "Rotating…".
 * @property revoking shown and announced while the revocation runs: "Revoking…".
 * @property rotateFailed shown when the rotation fails without a message: "The key was not rotated."
 * @property revokeFailed shown when the revocation fails without a message: "The key was not revoked."
 */
@Immutable
public class MonarchApiKeyRowLabels(
    public val prefix: ((prefix: String) -> String)? = null,
    public val created: String? = null,
    public val lastUsed: String? = null,
    public val neverUsed: String? = null,
    public val expires: String? = null,
    public val scopes: String? = null,
    public val noScopes: String? = null,
    public val moreScopes: ((count: Int) -> String)? = null,
    public val status: ((status: MonarchApiKeyStatus) -> String)? = null,
    public val menu: String? = null,
    public val actions: ((name: String) -> String)? = null,
    public val rotate: String? = null,
    public val revoke: String? = null,
    public val rotateTitle: ((name: String) -> String)? = null,
    public val rotateMessage: String? = null,
    public val rotateConfirm: String? = null,
    public val revokeTitle: ((name: String) -> String)? = null,
    public val revokeMessage: String? = null,
    public val revokeConfirm: String? = null,
    public val revokePhrasePrompt: ((phrase: String) -> String)? = null,
    public val cancel: String? = null,
    public val rotating: String? = null,
    public val revoking: String? = null,
    public val rotateFailed: String? = null,
    public val revokeFailed: String? = null,
)
