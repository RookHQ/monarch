/// The size of a ``MonarchAiVoiceMode``.
public enum MonarchAiVoiceModeSize: Sendable, Hashable, CaseIterable {
    /// Stands on its own and fills the space it is given, for a full-screen cover. The default.
    case md
    /// A card that fits inside an assistant panel, with small buttons.
    case compact
}
