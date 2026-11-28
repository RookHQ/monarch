/// Built-in strings of the roster, from the `MonarchAiMultiAgentRoster` strings table.
enum MonarchAiMultiAgentRosterStrings {
    private static let table = "MonarchAiMultiAgentRoster"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.aiMultiAgentRoster." + key, table: table)
    }

    private static func counted(_ key: String, _ count: Int) -> String {
        let suffix = count == 1 ? "One" : ""
        return MonarchStrings.string("monarch.aiMultiAgentRoster.\(key)\(suffix)", table: table, count)
    }

    /// Names the list or the stack.
    static var label: String { string("label") }
    /// Shown when there are no agents.
    static var empty: String { string("empty") }
    /// What separates the parts of the summary.
    static var separator: String { string("separator") }
    /// "3 agents".
    static func agents(_ count: Int) -> String { counted("agents", count) }
    /// "2 working".
    static func working(_ count: Int) -> String { counted("working", count) }
    /// "1 waiting for you".
    static func waiting(_ count: Int) -> String { counted("waiting", count) }
    /// "1 with errors".
    static func errors(_ count: Int) -> String { counted("errors", count) }

    /// "Research agent: Error".
    static func changed(_ name: String, _ state: String) -> String {
        MonarchStrings.string("monarch.aiMultiAgentRoster.changed", table: table, name, state)
    }

    /// A list row as read: "Coordinator (AI agent)".
    static func agentName(_ name: String, kind: String) -> String {
        MonarchStrings.string("monarch.aiMultiAgentRoster.agentName", table: table, name, kind)
    }

    /// One hidden agent behind "+N": "Research agent, Waiting for you".
    static func hiddenAgent(_ name: String, _ state: String) -> String {
        MonarchStrings.string("monarch.aiMultiAgentRoster.hiddenAgent", table: table, name, state)
    }
}
