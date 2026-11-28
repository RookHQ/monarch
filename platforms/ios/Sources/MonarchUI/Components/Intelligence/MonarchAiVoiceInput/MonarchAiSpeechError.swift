/// Why a ``MonarchAiSpeechRecognizer`` stopped. Throw it from the event stream.
@_spi(Experimental) public enum MonarchAiSpeechError: Error, Sendable, Hashable, CaseIterable {
    /// The person turned off microphone or speech recognition access.
    case denied
    /// Nothing was heard.
    case noSpeech
    /// No microphone is available.
    case noMicrophone
    /// Recognition is not available now, for example for this language or without a network.
    case unavailable
    /// Any other failure.
    case failed
}
