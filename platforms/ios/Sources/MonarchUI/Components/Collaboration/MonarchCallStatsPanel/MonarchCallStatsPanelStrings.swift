/// Built-in strings of the call stats panel, from the `MonarchCallStatsPanel` strings table.
enum MonarchCallStatsPanelStrings {
    private static let table = "MonarchCallStatsPanel"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.callStatsPanel.\(key)", table: table)
    }

    /// The heading.
    static var dataUsed: String { string("dataUsed") }
    /// The low data note.
    static var lowData: String { string("lowData") }
    /// The spoken name of the list of people.
    static var connections: String { string("connections") }
    /// The round-trip label.
    static var roundTrip: String { string("roundTrip") }
    /// The packet loss label.
    static var loss: String { string("loss") }
    /// Shown when no one else is on the call.
    static var noPeers: String { string("noPeers") }

    /// The name of a grade.
    static func quality(_ quality: MonarchCallStatsPanelQuality) -> String {
        switch quality {
        case .good: string("quality.good")
        case .fair: string("quality.fair")
        case .poor: string("quality.poor")
        case .unknown: string("quality.unknown")
        }
    }

    /// The usage line.
    static func usageSummary(total: String, up: String, down: String) -> String {
        MonarchStrings.string("monarch.callStatsPanel.usageSummary", table: table, total, up, down)
    }

    /// A rate in bits per second.
    static func bitsPerSecond(_ value: String) -> String {
        MonarchStrings.string("monarch.callStatsPanel.bps", table: table, value)
    }

    /// A rate in kilobits per second.
    static func kilobitsPerSecond(_ value: String) -> String {
        MonarchStrings.string("monarch.callStatsPanel.kbps", table: table, value)
    }

    /// A rate in megabits per second.
    static func megabitsPerSecond(_ value: String) -> String {
        MonarchStrings.string("monarch.callStatsPanel.mbps", table: table, value)
    }
    /// The Person column heading.
    static var person: String { string("person") }
    /// The Link column heading.
    static var link: String { string("link") }
}
