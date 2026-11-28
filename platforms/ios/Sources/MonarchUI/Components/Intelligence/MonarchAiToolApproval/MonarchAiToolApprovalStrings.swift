/// Built-in strings of the tool approval, from the `MonarchAiToolApproval` strings table.
enum MonarchAiToolApprovalStrings {
    private static let table = "MonarchAiToolApproval"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.aiToolApproval.\(key)", table: table)
    }

    static var eyebrow: String { string("eyebrow") }
    static var arguments: String { string("arguments") }
    static var noArguments: String { string("noArguments") }
    static var approve: String { string("approve") }
    static var deny: String { string("deny") }
    static var editArguments: String { string("editArguments") }
    static var discardEdits: String { string("discardEdits") }
    static var argumentsJSON: String { string("argumentsJSON") }
    static var invalidJSON: String { string("invalidJSON") }
    static var addReason: String { string("addReason") }
    static var reason: String { string("reason") }
    static var alwaysAllow: String { string("alwaysAllow") }
    static var approved: String { string("approved") }
    static var denied: String { string("denied") }
    static var cancelled: String { string("cancelled") }

    /// The risk badge text.
    static func risk(_ risk: MonarchAiToolCallRisk) -> String { string("risk.\(risk.rawValue)") }

    /// The built-in consequence line: only a destructive call has one.
    static func consequence(_ risk: MonarchAiToolCallRisk) -> String? {
        risk == .destructive ? string("consequence.destructive") : nil
    }

    /// Announced when the card appears.
    static func announce(_ title: String) -> String {
        MonarchStrings.string("monarch.aiToolApproval.announce", table: table, title)
    }
}
