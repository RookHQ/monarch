package dev.monarchui.components.intelligence.aiconnectorpicker

/** Whether a connector of a `MonarchAiConnectorPicker` can be used. Same names as iOS. */
public enum class MonarchAiConnectorStatus {
    /** Signed in and working: the connector can be turned on. */
    Connected,

    /** The person must sign in before the connector can be used. */
    NeedsSignIn,

    /** The connection failed and must be made again. */
    Error,

    /** The connector cannot be used right now, for example because an admin turned it off. */
    Unavailable,
}
