package com.rookhq.monarch.components.intelligence.aivoiceinput

/** How the `MonarchAiVoiceInput` button listens. The same names exist on every platform. */
public enum class MonarchAiVoiceInputMode {
    /** Press to start and press again to stop. The default. */
    Toggle,

    /** Listen while the button is held. TalkBack users, who cannot hold, double-tap to start and again to stop. */
    PushToTalk,
}
