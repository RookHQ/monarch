/// What a ``MonarchAiSpeechRecognizer`` reports while it listens.
@_spi(Experimental) public enum MonarchAiSpeechEvent: Sendable, Hashable {
    /// The microphone is open and recognition has begun.
    case started
    /// The input level, 0 through 1, for the level meter.
    case level(Double)
    /// A guess at the words so far. It may change; never send it on its own.
    case partial(String)
    /// Settled words. Each phrase arrives once.
    case final(String)
}
