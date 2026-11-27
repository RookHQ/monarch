/// Built-in strings of the suggested actions, from the `MonarchAiSuggestedActions` strings table.
enum MonarchAiSuggestedActionsStrings {
    private static let table = "MonarchAiSuggestedActions"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.aiSuggestedActions.\(key)", table: table)
    }

    private static func string(_ key: String, _ argument: String) -> String {
        MonarchStrings.string("monarch.aiSuggestedActions.\(key)", table: table, argument)
    }

    /// The heading.
    static var title: String { string("title") }
    /// The origin of AI suggestions.
    static var originAi: String { string("origin.ai") }
    /// The origin of rule-based suggestions.
    static var originRule: String { string("origin.rule") }
    /// The run button.
    static var run: String { string("run") }
    /// The spoken name of the run button.
    static func runAction(_ label: String) -> String { string("runAction", label) }
    /// The run button while running.
    static var running: String { string("running") }
    /// Shown after a run.
    static var done: String { string("done") }
    /// Shown under an action whose run failed.
    static var failed: String { string("failed") }
    /// The spoken name of the dismiss button.
    static func dismiss(_ label: String) -> String { string("dismiss", label) }
    /// The approval tag.
    static var needsApproval: String { string("needsApproval") }
    /// The risk tag, or `nil` for low risk.
    static func risk(_ risk: MonarchAiToolCallRisk) -> String? {
        switch risk {
        case .low: nil
        case .medium: string("risk.medium")
        case .high: string("risk.high")
        case .destructive: string("risk.destructive")
        }
    }
    /// The approval dialog's title.
    static func approvalTitle(_ label: String) -> String { string("approvalTitle", label) }
    /// The approval dialog's text when the action has no description.
    static var approvalDescription: String { string("approvalDescription") }
    /// The approval dialog's confirm button.
    static var approve: String { string("approve") }
    /// The approval dialog's cancel button.
    static var cancel: String { string("cancel") }
    /// Announced when an action finishes.
    static func doneAnnouncement(_ label: String) -> String { string("doneAnnouncement", label) }
    /// Shown when there are no actions.
    static var empty: String { string("empty") }
}
