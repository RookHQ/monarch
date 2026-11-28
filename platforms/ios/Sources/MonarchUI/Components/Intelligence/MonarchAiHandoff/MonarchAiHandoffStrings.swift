/// Built-in strings of the handoff marker, from the `MonarchAiHandoff` strings table.
enum MonarchAiHandoffStrings {
    private static let table = "MonarchAiHandoff"

    /// Names the marker.
    static var label: String { MonarchStrings.string("monarch.aiHandoff.label", table: table) }
    /// Read after an agent's name.
    static var agentKind: String { MonarchStrings.string("monarch.aiHandoff.agentKind", table: table) }
    /// Read before the reason.
    static var reason: String { MonarchStrings.string("monarch.aiHandoff.reason", table: table) }

    /// "Research agent handed off to Sam Lee".
    static func handedOff(_ from: String, _ to: String) -> String {
        MonarchStrings.string("monarch.aiHandoff.handedOff", table: table, from, to)
    }

    /// An agent's spoken name with its kind: "Research agent (AI agent)".
    static func agentName(_ name: String, kind: String) -> String {
        MonarchStrings.string("monarch.aiHandoff.agentName", table: table, name, kind)
    }
}
