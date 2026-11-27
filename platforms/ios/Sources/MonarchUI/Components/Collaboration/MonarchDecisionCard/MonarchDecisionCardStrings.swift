/// Built-in strings of the decision card, from the `MonarchDecisionCard`
/// strings table.
enum MonarchDecisionCardStrings {
    private static let table = "MonarchDecisionCard"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.decisionCard." + key, table: table)
    }

    /// The spoken name of the group of choices.
    static var choices: String { string("choices") }
    /// The label of the choice that clears the decision.
    static var none: String { string("none") }
    /// The label of the reason field when a reason is required.
    static var reason: String { string("reason") }
    /// The label of the reason field when a reason is optional.
    static var reasonOptional: String { string("reasonOptional") }
    /// The placeholder of the reason field.
    static var reasonPrompt: String { string("reasonPrompt") }
    /// Shown when a required reason is missing.
    static var reasonRequired: String { string("reasonRequired") }
    /// The title of the save button.
    static var save: String { string("save") }
    /// Announced after saving.
    static var saved: String { string("saved") }
    /// The read-only text when nothing has been decided.
    static var undecided: String { string("undecided") }
    /// Shown when saving fails without a message.
    static var failed: String { string("failed") }

    /// The characters left before the limit of the reason.
    static func charactersLeft(_ count: Int) -> String {
        count == 1
            ? string("charactersLeft.one")
            : MonarchStrings.string("monarch.decisionCard.charactersLeft.other", table: table, count)
    }

    /// When the decision was made.
    static func decidedAt(_ time: String) -> String {
        MonarchStrings.string("monarch.decisionCard.decidedAt", table: table, time)
    }

    /// Who made the decision.
    static func decidedBy(_ name: String) -> String {
        MonarchStrings.string("monarch.decisionCard.decidedBy", table: table, name)
    }

    /// When and by whom the decision was made.
    static func decidedAtBy(_ time: String, _ name: String) -> String {
        MonarchStrings.string("monarch.decisionCard.decidedAtBy", table: table, time, name)
    }
}
