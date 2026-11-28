/// How the button of ``MonarchAiVoiceInput`` starts and stops dictation.
@_spi(Experimental) public enum MonarchAiVoiceInputMode: Sendable, Hashable, CaseIterable {
    /// Press to start and press again to stop. The default.
    case toggle
    /// Listen while the button is held; release to stop.
    case pushToTalk
}
