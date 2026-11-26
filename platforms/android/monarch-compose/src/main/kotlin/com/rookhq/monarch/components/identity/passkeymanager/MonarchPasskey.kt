package com.rookhq.monarch.components.identity.passkeymanager

import androidx.compose.runtime.Immutable

/**
 * A passkey of the person, for `MonarchPasskeyManager`.
 *
 * @property id a stable id, passed to `onRename` and `onRemove`.
 * @property name the name the person gave it, or the provider's name, such as "Google Password Manager".
 * @property createdMillis when it was added, in epoch milliseconds (UTC).
 * @property lastUsedMillis when it was last used to sign in, in epoch milliseconds (UTC). `null` reads "Never used".
 * @property synced whether a password manager syncs it across the person's devices.
 * @property detail a short extra line, such as the device it was made on.
 */
@Immutable
public class MonarchPasskey(
    public val id: String,
    public val name: String,
    public val createdMillis: Long? = null,
    public val lastUsedMillis: Long? = null,
    public val synced: Boolean = false,
    public val detail: String? = null,
)
