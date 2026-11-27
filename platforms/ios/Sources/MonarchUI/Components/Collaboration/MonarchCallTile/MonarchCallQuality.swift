/// The connection quality of a call participant, from the product's call statistics.
public enum MonarchCallQuality: Sendable, Hashable, CaseIterable {
    /// Three bars.
    case good
    /// Two bars: an unstable connection.
    case fair
    /// One bar.
    case poor
}
