/// The colour of the arc of a ``MonarchScoreRing``.
public enum MonarchScoreRingColor: Sendable, Hashable {
    /// The tone of the band the score falls in, from the thresholds. The default.
    case auto
    /// The accent colour, for a score with no good or bad meaning.
    case accent
    /// A fixed tone.
    case tone(MonarchTone)
}
