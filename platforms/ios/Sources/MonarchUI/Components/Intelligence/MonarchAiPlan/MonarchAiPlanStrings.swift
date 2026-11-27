/// Built-in strings of the plan, from the `MonarchAiPlan` strings table.
enum MonarchAiPlanStrings {
    private static let table = "MonarchAiPlan"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.aiPlan.\(key)", table: table)
    }

    /// The title above the steps.
    static var title: String { string("title") }
    /// The explanation while the plan waits.
    static var description: String { string("description") }
    /// Added while the plan can be edited.
    static var editHint: String { string("editHint") }
    /// Shown once approved.
    static var approvedNotice: String { string("approvedNotice") }
    /// Shown once rejected.
    static var rejectedNotice: String { string("rejectedNotice") }
    /// The placeholder of an empty step.
    static var stepPlaceholder: String { string("stepPlaceholder") }
    /// The add button.
    static var addStep: String { string("addStep") }
    /// The approve button.
    static var approve: String { string("approve") }
    /// The reject button.
    static var reject: String { string("reject") }
    /// Shown while every step is empty.
    static var emptyPlan: String { string("emptyPlan") }

    /// The spoken name of a step's text.
    static func stepLabel(_ position: Int) -> String {
        MonarchStrings.string("monarch.aiPlan.stepLabel", table: table, position)
    }

    /// The name of an empty step.
    static func untitledStep(_ position: Int) -> String {
        MonarchStrings.string("monarch.aiPlan.untitledStep", table: table, position)
    }

    /// The spoken name of a drag handle.
    static func reorder(_ label: String) -> String {
        MonarchStrings.string("monarch.aiPlan.reorder", table: table, label)
    }

    /// Announced after a move.
    static func moved(_ label: String, position: Int, total: Int) -> String {
        MonarchStrings.string("monarch.aiPlan.moved", table: table, label, position, total)
    }

    /// The spoken name of a remove button.
    static func remove(_ label: String) -> String {
        MonarchStrings.string("monarch.aiPlan.remove", table: table, label)
    }

    /// Announced after a removal.
    static func removed(_ label: String) -> String {
        MonarchStrings.string("monarch.aiPlan.removed", table: table, label)
    }
}
