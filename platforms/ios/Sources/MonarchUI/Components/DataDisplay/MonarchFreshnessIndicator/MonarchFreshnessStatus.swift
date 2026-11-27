/// How fresh a data set or a sync is.
public enum MonarchFreshnessStatus: Sendable, Hashable, CaseIterable {
    /// Updated within the stale limit. Drawn with the success tone.
    case fresh
    /// Older than the stale limit. Drawn with the warning tone and the words
    /// "Out of date".
    case stale
    /// Older than the expired limit. Drawn with the danger tone and the words
    /// "Very out of date".
    case expired
    /// Never updated. Drawn with the neutral tone.
    case never
    /// The status itself is being read. Drawn with the info tone.
    case checking
    /// The status could not be read, or the last sync failed. Drawn with the
    /// danger tone and the error text.
    case error
}
