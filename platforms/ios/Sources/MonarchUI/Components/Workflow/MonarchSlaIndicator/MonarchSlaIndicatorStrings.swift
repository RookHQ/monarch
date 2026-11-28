/// Built-in strings of the SLA indicator, from the `MonarchSlaIndicator`
/// strings table.
enum MonarchSlaIndicatorStrings {
    private static let table = "MonarchSlaIndicator"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.slaIndicator." + key, table: table)
    }

    /// The name of a state.
    static func name(of state: MonarchSlaIndicatorState) -> String {
        switch state {
        case .onTrack: string("onTrack")
        case .atRisk: string("atRisk")
        case .breached: string("breached")
        case .paused: string("paused")
        case .met: string("met")
        case .notApplicable: string("notApplicable")
        }
    }

    /// Shown when no target applies and the name is not wanted.
    static var notApplicableMark: String { string("notApplicableMark") }
    /// A duration shorter than one minute, shown.
    static var lessThanMinute: String { string("lessThanMinute") }
    /// A duration shorter than one minute, spoken.
    static var lessThanMinuteSpoken: String { string("lessThanMinuteSpoken") }
    /// Read when an earlier target of the same item was breached.
    static var earlierBreach: String { string("earlierBreach") }

    /// The time left before the target is due.
    static func remaining(_ duration: String) -> String {
        MonarchStrings.string("monarch.slaIndicator.remaining", table: table, duration)
    }

    /// The time since the target was due.
    static func overdue(_ duration: String) -> String {
        MonarchStrings.string("monarch.slaIndicator.overdue", table: table, duration)
    }

    /// The date and time the target is due.
    static func due(_ date: String) -> String {
        MonarchStrings.string("monarch.slaIndicator.due", table: table, date)
    }

    /// The announcement of a new state, with the name of the target if it
    /// has one.
    static func changed(_ state: String, target: String?) -> String {
        if let target {
            return MonarchStrings.string("monarch.slaIndicator.changedTarget", table: table, target, state)
        }
        return MonarchStrings.string("monarch.slaIndicator.changed", table: table, state)
    }
}
