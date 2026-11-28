/// The unit of the compact age of a ``MonarchInboxRow``, for translating it.
public enum MonarchInboxRowAgeUnit: Sendable, Hashable, CaseIterable {
    /// Less than a minute: "now". The number is 0.
    case now
    /// Minutes: "5m".
    case minutes
    /// Hours: "3h".
    case hours
    /// Days: "2d".
    case days
    /// Weeks: "4w".
    case weeks
}
