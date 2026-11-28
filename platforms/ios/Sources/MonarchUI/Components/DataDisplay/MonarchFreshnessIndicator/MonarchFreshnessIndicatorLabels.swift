/// Replacements for the built-in strings of ``MonarchFreshnessIndicator``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchFreshnessIndicatorLabels: Sendable {
    /// The text before the time. Receives the relative time, such as
    /// "3 minutes ago". Built in: "Updated 3 minutes ago".
    public var updated: (@Sendable (String) -> String)?
    /// Shown when there is no update time. Built in: "Never updated".
    public var never: String?
    /// Shown while the status is being read. Built in: "Checking…".
    public var checking: String?
    /// The words after the time when the data is stale. Built in: "Out of date".
    public var stale: String?
    /// The words after the time when the data is expired. Built in:
    /// "Very out of date".
    public var expired: String?
    /// Shown for the error status when no error text is given. Built in:
    /// "Status unavailable".
    public var error: String?
    /// The title of the refresh button. Built in: "Refresh".
    public var refresh: String?
    /// The title of the refresh button while it runs. Built in: "Refreshing…".
    public var refreshing: String?
    /// Announced when a refresh finishes. Built in: "Refreshed."
    public var refreshed: String?
    /// Announced when a refresh throws. Built in: "Refresh failed."
    public var refreshFailed: String?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        updated: (@Sendable (String) -> String)? = nil,
        never: String? = nil,
        checking: String? = nil,
        stale: String? = nil,
        expired: String? = nil,
        error: String? = nil,
        refresh: String? = nil,
        refreshing: String? = nil,
        refreshed: String? = nil,
        refreshFailed: String? = nil
    ) {
        self.updated = updated
        self.never = never
        self.checking = checking
        self.stale = stale
        self.expired = expired
        self.error = error
        self.refresh = refresh
        self.refreshing = refreshing
        self.refreshed = refreshed
        self.refreshFailed = refreshFailed
    }
}
