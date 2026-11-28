/// Built-in strings of the issue card, from the `MonarchIssueCard` strings table.
enum MonarchIssueCardStrings {
    private static let table = "MonarchIssueCard"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.issueCard.\(key)", table: table)
    }

    private static func string(_ key: String, _ argument: String) -> String {
        MonarchStrings.string("monarch.issueCard.\(key)", table: table, argument)
    }

    /// "Overdue".
    static var overdue: String { string("overdue") }
    /// The spoken name of the tags.
    static var tags: String { string("tags") }
    /// "Unassigned".
    static var unassigned: String { string("unassigned") }
    /// "Selected".
    static var selected: String { string("selected") }

    /// "Due …".
    static func due(_ date: String) -> String { string("due", date) }
    /// "Assigned to …".
    static func assignee(_ name: String) -> String { string("assignee", name) }
    /// "Status: …".
    static func status(_ name: String) -> String { string("status", name) }
    /// "Priority: …".
    static func priority(_ level: String) -> String { string("priority", level) }

    /// The name of a priority level.
    static func priorityLevel(_ priority: MonarchIssueCardPriority) -> String {
        switch priority {
        case .urgent: string("priority.urgent")
        case .high: string("priority.high")
        case .medium: string("priority.medium")
        case .low: string("priority.low")
        case .none: string("priority.none")
        }
    }
}
