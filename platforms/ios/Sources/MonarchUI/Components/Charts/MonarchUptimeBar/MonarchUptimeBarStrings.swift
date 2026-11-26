/// Built-in strings of the uptime bar, from the `MonarchUptimeBar` strings table.
enum MonarchUptimeBarStrings {
    private static let table = "MonarchUptimeBar"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.uptimeBar.\(key)", table: table)
    }

    /// The name of a state.
    static func status(_ status: MonarchUptimeBarStatus) -> String {
        switch status {
        case .operational: string("status.operational")
        case .degraded: string("status.degraded")
        case .partial: string("status.partial")
        case .down: string("status.down")
        case .maintenance: string("status.maintenance")
        case .noData: string("status.noData")
        }
    }

    /// The name of the uptime value.
    static var uptime: String { string("uptime") }
    /// The name of the period axis.
    static var period: String { string("period") }
    /// The name of the state axis.
    static var status: String { string("status") }
    /// The spoken name of the legend.
    static var legend: String { string("legend") }

    /// The overall uptime under the bar.
    static func overall(_ percent: String) -> String {
        MonarchStrings.string("monarch.uptimeBar.overall", table: table, percent)
    }

    /// The number of periods, first in the summary.
    static func periods(_ count: String) -> String {
        MonarchStrings.string("monarch.uptimeBar.periods", table: table, count)
    }
}
