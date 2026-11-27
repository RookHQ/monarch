package com.rookhq.monarch.components.intelligence.aivoiceinput

/** Where dictation is. The same names exist on every platform. */
public enum class MonarchAiVoiceInputState {
    /** Ready to listen. */
    Idle,

    /** Waiting for the permission and the microphone. */
    Starting,

    /** Hearing speech. */
    Listening,

    /** Microphone access is refused. */
    Denied,

    /** Something else went wrong. */
    Error,
}
