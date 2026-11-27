/// Built-in strings of the freshness indicator, from the
/// `MonarchFreshnessIndicator` strings table.
enum MonarchFreshnessIndicatorStrings {
    private static let table = "MonarchFreshnessIndicator"

    /// "Updated 3 minutes ago"; the argument is the relative time.
    static func updated(_ time: String) -> String {
        MonarchStrings.string("monarch.freshnessIndicator.updated", table: table, time)
    }
    /// Shown when there is no update time.
    static var never: String { MonarchStrings.string("monarch.freshnessIndicator.never", table: table) }
    /// Shown while the status is being read.
    static var checking: String { MonarchStrings.string("monarch.freshnessIndicator.checking", table: table) }
    /// The words after the time when the data is stale.
    static var stale: String { MonarchStrings.string("monarch.freshnessIndicator.stale", table: table) }
    /// The words after the time when the data is expired.
    static var expired: String { MonarchStrings.string("monarch.freshnessIndicator.expired", table: table) }
    /// Shown for the error status when the app gives no error text.
    static var error: String { MonarchStrings.string("monarch.freshnessIndicator.error", table: table) }
    /// The title of the refresh button.
    static var refresh: String { MonarchStrings.string("monarch.freshnessIndicator.refresh", table: table) }
    /// The title of the refresh button while it runs.
    static var refreshing: String { MonarchStrings.string("monarch.freshnessIndicator.refreshing", table: table) }
    /// Announced when a refresh finishes.
    static var refreshed: String { MonarchStrings.string("monarch.freshnessIndicator.refreshed", table: table) }
    /// Announced when a refresh fails.
    static var refreshFailed: String {
        MonarchStrings.string("monarch.freshnessIndicator.refreshFailed", table: table)
    }
}
