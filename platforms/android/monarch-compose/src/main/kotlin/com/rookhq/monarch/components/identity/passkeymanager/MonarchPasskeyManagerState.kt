package com.rookhq.monarch.components.identity.passkeymanager

/** The loading state of a `MonarchPasskeyManager`. */
public enum class MonarchPasskeyManagerState {
    /** The passkeys are loaded. An empty list shows the empty state. */
    Ready,

    /** The passkeys are loading. */
    Loading,

    /** Loading failed. */
    Failed,
}
