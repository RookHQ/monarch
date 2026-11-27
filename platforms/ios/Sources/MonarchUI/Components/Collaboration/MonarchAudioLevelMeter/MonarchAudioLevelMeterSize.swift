/// The size of the segments of an audio level meter, before Dynamic Type is applied.
public enum MonarchAudioLevelMeterSize: Sendable, Hashable, CaseIterable {
    /// 12 points high.
    case sm
    /// 16 points high. The default.
    case md
}
