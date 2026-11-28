/// Where a spoken conversation in ``MonarchAiVoiceMode`` is. The app reports
/// it; the component only shows it.
public enum MonarchAiVoiceModeState: Sendable, Hashable, CaseIterable {
    /// The call is being set up.
    case connecting
    /// The assistant hears the person.
    case listening
    /// The assistant is preparing a reply.
    case thinking
    /// The assistant is talking.
    case speaking
    /// The person's microphone is off.
    case muted
    /// The connection dropped and is being restored.
    case reconnecting
    /// The call is over.
    case ended
    /// The call failed.
    case error
}
