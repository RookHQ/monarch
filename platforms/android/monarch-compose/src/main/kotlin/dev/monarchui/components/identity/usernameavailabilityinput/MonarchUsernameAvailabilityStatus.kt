package dev.monarchui.components.identity.usernameavailabilityinput

/** Where the check of a `MonarchUsernameAvailabilityInput` stands. The same five names exist on every platform. */
public enum class MonarchUsernameAvailabilityStatus {
    /** Nothing to check: the field is empty, or the check returned `null`. */
    Idle,

    /** The check is waiting for typing to pause, or running. */
    Checking,

    /** The value can be used. */
    Valid,

    /** The value cannot be used. */
    Invalid,

    /** The check failed, for example because the network did; it can be tried again. */
    Error,
}
