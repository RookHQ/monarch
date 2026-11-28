/// Replacements for the built-in strings of ``MonarchAiVoiceMode``.
///
/// Every property is `nil` (or empty) by default, which keeps the built-in string.
public struct MonarchAiVoiceModeLabels: Sendable {
    /// The spoken name of the surface, from the assistant's name. Built in: "Voice conversation with Ada".
    public var label: (@Sendable (String) -> String)?
    /// The name used for the assistant when no agent is given. Built in: "Assistant".
    public var assistant: String?
    /// The speaker name on the person's own captions. Built in: "You".
    public var you: String?
    /// The always-visible state text, also announced. Pass only the states to replace.
    public var states: [MonarchAiVoiceModeState: String]
    /// Shown beside the state when the microphone is muted in another state. Built in: "Your microphone is muted".
    public var microphoneMuted: String?
    /// The mute toggle. Built in: "Mute".
    public var mute: String?
    /// The interrupt button. Built in: "Interrupt".
    public var interrupt: String?
    /// The captions toggle. Built in: "Captions".
    public var captions: String?
    /// The end button. Built in: "End".
    public var end: String?
    /// The button on the ended state. Built in: "Start again".
    public var restart: String?
    /// The button on the error state. Built in: "Retry".
    public var retry: String?
    /// Shown in the captions before anyone has spoken. Built in: "Captions appear here as you talk."
    public var captionsEmpty: String?
    /// Read before the running time. Built in: "Call time".
    public var elapsed: String?
    /// The duration on the ended state, from the formatted time. Built in: "Lasted 4:05".
    public var lasted: (@Sendable (String) -> String)?
    /// The error text when no error is given. Built in: "The voice conversation stopped unexpectedly."
    public var errorFallback: String?

    /// Replacements for built-in strings. Pass only the ones to replace.
    public init(
        label: (@Sendable (String) -> String)? = nil,
        assistant: String? = nil,
        you: String? = nil,
        states: [MonarchAiVoiceModeState: String] = [:],
        microphoneMuted: String? = nil,
        mute: String? = nil,
        interrupt: String? = nil,
        captions: String? = nil,
        end: String? = nil,
        restart: String? = nil,
        retry: String? = nil,
        captionsEmpty: String? = nil,
        elapsed: String? = nil,
        lasted: (@Sendable (String) -> String)? = nil,
        errorFallback: String? = nil
    ) {
        self.label = label
        self.assistant = assistant
        self.you = you
        self.states = states
        self.microphoneMuted = microphoneMuted
        self.mute = mute
        self.interrupt = interrupt
        self.captions = captions
        self.end = end
        self.restart = restart
        self.retry = retry
        self.captionsEmpty = captionsEmpty
        self.elapsed = elapsed
        self.lasted = lasted
        self.errorFallback = errorFallback
    }
}
