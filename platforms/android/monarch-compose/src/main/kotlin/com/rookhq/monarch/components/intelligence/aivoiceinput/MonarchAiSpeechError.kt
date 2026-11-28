package com.rookhq.monarch.components.intelligence.aivoiceinput

/** Why a listening session failed. The same names exist on every platform. */
public enum class MonarchAiSpeechError {
    /** Microphone or speech recognition access is refused. */
    Denied,

    /** Nothing was heard. */
    NoSpeech,

    /** No microphone is available, or it could not be opened. */
    NoMicrophone,

    /** Recognition is not available now: no network, an unsupported language, a busy service. */
    Unavailable,

    /** Any other failure. */
    Failed,
}
