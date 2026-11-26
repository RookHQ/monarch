/// Built-in strings of the usage meter, from the `MonarchUsageMeter` strings table.
enum MonarchUsageMeterStrings {
    private static let table = "MonarchUsageMeter"

    /// The figures of a limited item.
    static func amount(used: String, limit: String) -> String {
        MonarchStrings.string("monarch.usageMeter.amount", table: table, used, limit)
    }

    /// The share used.
    static func percent(_ percent: String) -> String {
        MonarchStrings.string("monarch.usageMeter.percent", table: table, percent)
    }

    /// Shown when an item is near its limit.
    static var nearLimit: String { MonarchStrings.string("monarch.usageMeter.nearLimit", table: table) }
    /// Shown when an item is over its limit.
    static var overLimit: String { MonarchStrings.string("monarch.usageMeter.overLimit", table: table) }

    /// The reset line.
    static func resets(_ date: String) -> String {
        MonarchStrings.string("monarch.usageMeter.resets", table: table, date)
    }

    /// Announced when an item reaches a level.
    static func announcement(label: String, level: String) -> String {
        MonarchStrings.string("monarch.usageMeter.announcement", table: table, label, level)
    }

    /// Shown while loading.
    static var loading: String { MonarchStrings.string("monarch.usageMeter.loading", table: table) }
    /// The title of the retry button.
    static var retry: String { MonarchStrings.string("monarch.usageMeter.retry", table: table) }
}
