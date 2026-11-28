package dev.monarchui.components.intelligence.aivoicemode

/** Where a spoken conversation is, as the app reports it. The same names exist on every platform. */
public enum class MonarchAiVoiceModeState {
    /** The call is being set up. */
    Connecting,

    /** The assistant hears the person. */
    Listening,

    /** The assistant is preparing a reply. */
    Thinking,

    /** The assistant is talking. */
    Speaking,

    /** The person's microphone is off. */
    Muted,

    /** The connection dropped and is being restored. */
    Reconnecting,

    /** The call is over. */
    Ended,

    /** The call failed. */
    Error,
}
