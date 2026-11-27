/// Built-in strings of the escalation policy editor, from the `MonarchEscalationPolicyEditor` strings table.
enum MonarchEscalationPolicyEditorStrings {
    private static let table = "MonarchEscalationPolicyEditor"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.escalationPolicyEditor.\(key)", table: table)
    }

    private static func string(_ key: String, _ number: Int) -> String {
        MonarchStrings.string("monarch.escalationPolicyEditor.\(key)", table: table, number)
    }

    static var label: String { string("label") }
    static var after: String { string("after") }
    static var notify: String { string("notify") }
    static var nobody: String { string("nobody") }
    static var add: String { string("add") }
    static var noTargets: String { string("noTargets") }

    static func level(_ position: Int) -> String { string("level", position) }
    static func remove(_ position: Int) -> String { string("remove", position) }
    static func maxReached(_ count: Int) -> String { string("maxReached", count) }

    /// The text of a problem.
    static func problem(_ problem: MonarchEscalationLevelProblem) -> String {
        switch problem {
        case .targets: string("problem.targets")
        case .order: string("problem.order")
        }
    }

    /// The summary of a level.
    static func summary(_ duration: String, _ names: String) -> String {
        MonarchStrings.string("monarch.escalationPolicyEditor.summary", table: table, duration, names)
    }
}
