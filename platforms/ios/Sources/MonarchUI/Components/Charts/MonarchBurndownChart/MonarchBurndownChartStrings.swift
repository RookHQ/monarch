/// Built-in strings of the burndown chart, from the `MonarchBurndownChart` strings table.
enum MonarchBurndownChartStrings {
    private static let table = "MonarchBurndownChart"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.burndownChart." + key, table: table)
    }

    private static func format(_ key: String, _ arguments: any CVarArg...) -> String {
        String(format: string(key), arguments: arguments)
    }

    /// The name of the actual line.
    static var remaining: String { string("remaining") }
    /// The name of the ideal line.
    static var ideal: String { string("ideal") }
    /// The name of the scope line.
    static var scope: String { string("scope") }
    /// The heading of the day column of the data table.
    static var day: String { string("day") }
    /// The label of the today marker.
    static var today: String { string("today") }
    /// The heading of the scope change column.
    static var scopeChange: String { string("scopeChange") }
    /// A scope change with its sign.
    static func change(_ delta: String, added: Bool) -> String {
        format(added ? "change.added" : "change.removed", delta)
    }
    /// Where the sprint stands on the latest day with data.
    static func status(day: String, remaining: String, ideal: String) -> String {
        format("status", day, remaining, ideal)
    }
    /// Ahead of the ideal line by an amount.
    static func ahead(_ amount: String) -> String { format("ahead", amount) }
    /// Behind the ideal line by an amount.
    static func behind(_ amount: String) -> String { format("behind", amount) }
    /// On the ideal line.
    static var onTrack: String { string("onTrack") }
    /// The scope and how much was added and removed.
    static func scopeSummary(scope: String, added: String, removed: String) -> String {
        format("scopeSummary", scope, added, removed)
    }
    /// The summary before any remaining value.
    static var notStarted: String { string("notStarted") }
}
