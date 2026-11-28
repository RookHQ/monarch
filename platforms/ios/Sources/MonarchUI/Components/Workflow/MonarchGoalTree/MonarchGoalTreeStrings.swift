import Foundation

/// Built-in strings of the goal tree, from the `MonarchGoalTree` strings table.
enum MonarchGoalTreeStrings {
    private static let table = "MonarchGoalTree"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.goalTree.\(key)", table: table)
    }

    private static func string(_ key: String, _ argument: String) -> String {
        MonarchStrings.string("monarch.goalTree.\(key)", table: table, argument)
    }

    /// "Aligned to".
    static var alignedTo: String { string("alignedTo") }
    /// The add-child button.
    static var addChild: String { string("addChild") }
    /// Shown when there are no goals.
    static var empty: String { string("empty") }

    /// The name of a status.
    static func status(_ status: MonarchGoalStatus) -> String {
        switch status {
        case .notStarted: string("status.notStarted")
        case .onTrack: string("status.onTrack")
        case .atRisk: string("status.atRisk")
        case .offTrack: string("status.offTrack")
        case .done: string("status.done")
        }
    }

    /// "Expand …".
    static func expand(_ title: String) -> String { string("expand", title) }
    /// "Collapse …".
    static func collapse(_ title: String) -> String { string("collapse", title) }
    /// "Add key result to …".
    static func addChildTo(_ title: String) -> String { string("addChildTo", title) }

    /// "1 key result" or "3 key results".
    static func childCount(_ count: Int) -> String {
        string(count == 1 ? "childCount.one" : "childCount.other", count.formatted())
    }
}
