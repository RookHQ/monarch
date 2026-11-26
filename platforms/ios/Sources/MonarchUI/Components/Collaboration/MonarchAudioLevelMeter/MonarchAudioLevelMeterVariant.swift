/// How an audio level meter is drawn.
public enum MonarchAudioLevelMeterVariant: Sendable, Hashable, CaseIterable {
    /// Rising segments, the size of a word. The default.
    case segments
    /// One thin bar that fills the available width.
    case bar
}
