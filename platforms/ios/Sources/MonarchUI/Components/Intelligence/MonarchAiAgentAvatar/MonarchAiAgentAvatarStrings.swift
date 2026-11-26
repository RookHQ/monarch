/// Built-in strings of the agent avatar, from the `MonarchAiAgentAvatar` strings table.
enum MonarchAiAgentAvatarStrings {
    private static let table = "MonarchAiAgentAvatar"

    /// Read after the agent's name.
    static var kind: String { MonarchStrings.string("monarch.aiAgentAvatar.kind", table: table) }

    /// The name of a state.
    static func state(_ state: MonarchAiAgentState) -> String {
        MonarchStrings.string("monarch.aiAgentAvatar.state.\(state.rawValue)", table: table)
    }

    /// The spoken name of an idle agent: its name and its kind.
    static func label(name: String, kind: String) -> String {
        MonarchStrings.string("monarch.aiAgentAvatar.label", table: table, name, kind)
    }

    /// The spoken name of an agent that is doing something: its name, its kind and its state.
    static func labelWithState(name: String, kind: String, state: String) -> String {
        MonarchStrings.string("monarch.aiAgentAvatar.labelWithState", table: table, name, kind, state)
    }
}
