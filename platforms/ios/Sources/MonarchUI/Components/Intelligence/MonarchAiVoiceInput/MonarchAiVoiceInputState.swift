/// Where dictation in ``MonarchAiVoiceInput`` is.
@_spi(Experimental) public enum MonarchAiVoiceInputState: Sendable, Hashable, CaseIterable {
    /// Ready to listen.
    case idle
    /// Waiting for permission and the microphone.
    case starting
    /// Hearing speech.
    case listening
    /// Microphone or speech recognition access is off.
    case denied
    /// Something else went wrong: nothing was heard, no microphone, or the recogniser failed.
    case error
}
