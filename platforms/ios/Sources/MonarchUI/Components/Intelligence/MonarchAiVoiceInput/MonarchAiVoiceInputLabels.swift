/// Replacements for the built-in strings of ``MonarchAiVoiceInput``.
///
/// Every property is `nil` by default, which keeps the built-in string.
@_spi(Experimental) public struct MonarchAiVoiceInputLabels: Sendable, Hashable {
    /// The button in `toggle` mode. Built in: "Dictate".
    public var dictate: String?
    /// The button in `pushToTalk` mode. Built in: "Hold to talk".
    public var holdToTalk: String?
    /// The spoken hint of the `pushToTalk` button. Built in: "Double-tap to start listening, and again to stop."
    public var holdHint: String?
    /// The status while waiting for the microphone. Built in: "Starting microphone…".
    public var starting: String?
    /// The status while listening, also announced. Built in: "Listening…".
    public var listening: String?
    /// Announced when listening stops. Built in: "Stopped listening."
    public var stopped: String?
    /// Shown when access is off. Built in: "Microphone or speech recognition access is off. …".
    public var denied: String?
    /// Shown when nothing was heard. Built in: "Nothing was heard. Try again and speak clearly."
    public var noSpeech: String?
    /// Shown when there is no microphone. Built in: "No microphone is available. …".
    public var noMicrophone: String?
    /// Shown when recognition is not available. Built in: "Dictation isn’t available right now. …".
    public var unavailable: String?
    /// Shown for any other failure. Built in: "Dictation stopped unexpectedly. Try again."
    public var error: String?
    /// Read before the live transcript. Built in: "Transcript".
    public var transcript: String?
    /// The action that opens the app's page in Settings when access is off. Built in: "Open Settings".
    public var openSettings: String?

    /// Replacements for built-in strings. Pass only the ones to replace.
    public init(
        dictate: String? = nil,
        holdToTalk: String? = nil,
        holdHint: String? = nil,
        starting: String? = nil,
        listening: String? = nil,
        stopped: String? = nil,
        denied: String? = nil,
        noSpeech: String? = nil,
        noMicrophone: String? = nil,
        unavailable: String? = nil,
        error: String? = nil,
        transcript: String? = nil,
        openSettings: String? = nil
    ) {
        self.dictate = dictate
        self.holdToTalk = holdToTalk
        self.holdHint = holdHint
        self.starting = starting
        self.listening = listening
        self.stopped = stopped
        self.denied = denied
        self.noSpeech = noSpeech
        self.noMicrophone = noMicrophone
        self.unavailable = unavailable
        self.error = error
        self.transcript = transcript
        self.openSettings = openSettings
    }
}
