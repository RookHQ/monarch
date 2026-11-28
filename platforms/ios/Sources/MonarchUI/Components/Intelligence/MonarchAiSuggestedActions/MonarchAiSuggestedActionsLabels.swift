import Foundation

/// Replacements for the built-in strings of ``MonarchAiSuggestedActions``.
///
/// Every property is `nil` by default, which keeps the built-in string.
@_spi(Experimental) public struct MonarchAiSuggestedActionsLabels: Sendable {
    /// The origin of AI suggestions. Built in: "Suggested by AI".
    public var originAi: String?
    /// The origin of rule-based suggestions. Built in: "Rule-based".
    public var originRule: String?
    /// The run button. Built in: "Run".
    public var run: String?
    /// The spoken name of the run button, from the action. Built in: "Run “…”".
    public var runAction: (@Sendable (_ label: String) -> String)?
    /// The run button while running. Built in: "Running…".
    public var running: String?
    /// Shown after a run. Built in: "Done".
    public var done: String?
    /// Shown under an action whose run failed. Built in: "That didn’t work. Try again."
    public var failed: String?
    /// The spoken name of the dismiss button, from the action. Built in: "Dismiss “…”".
    public var dismiss: (@Sendable (_ label: String) -> String)?
    /// The approval tag. Built in: "Needs approval".
    public var needsApproval: String?
    /// The risk tag; return `nil` for the built-in text. Low risk has no tag.
    public var risk: (@Sendable (MonarchAiToolCallRisk) -> String?)?
    /// The approval dialog's title, from the action. Built in: "Run “…”?".
    public var approvalTitle: (@Sendable (_ label: String) -> String)?
    /// The approval dialog's text when the action has no description.
    public var approvalDescription: String?
    /// The approval dialog's confirm button when the action has no `confirmLabel`. Built in: "Run action".
    public var approve: String?
    /// The approval dialog's cancel button. Built in: "Cancel".
    public var cancel: String?
    /// Announced when an action finishes, from the action. Built in: "“…” done."
    public var doneAnnouncement: (@Sendable (_ label: String) -> String)?
    /// Shown when there are no actions. Built in: "No suggestions right now."
    public var empty: String?

    /// Replacements for built-in strings. Pass only the ones to replace.
    ///
    /// - Parameters:
    ///   - originAi: The origin of AI suggestions.
    ///   - originRule: The origin of rule-based suggestions.
    ///   - run: The run button.
    ///   - runAction: The spoken name of the run button.
    ///   - running: The run button while running.
    ///   - done: Shown after a run.
    ///   - failed: Shown under an action whose run failed.
    ///   - dismiss: The spoken name of the dismiss button.
    ///   - needsApproval: The approval tag.
    ///   - risk: The risk tag.
    ///   - approvalTitle: The approval dialog's title.
    ///   - approvalDescription: The approval dialog's text when the action has no description.
    ///   - approve: The approval dialog's confirm button.
    ///   - cancel: The approval dialog's cancel button.
    ///   - doneAnnouncement: Announced when an action finishes.
    ///   - empty: Shown when there are no actions.
    public init(
        originAi: String? = nil,
        originRule: String? = nil,
        run: String? = nil,
        runAction: (@Sendable (_ label: String) -> String)? = nil,
        running: String? = nil,
        done: String? = nil,
        failed: String? = nil,
        dismiss: (@Sendable (_ label: String) -> String)? = nil,
        needsApproval: String? = nil,
        risk: (@Sendable (MonarchAiToolCallRisk) -> String?)? = nil,
        approvalTitle: (@Sendable (_ label: String) -> String)? = nil,
        approvalDescription: String? = nil,
        approve: String? = nil,
        cancel: String? = nil,
        doneAnnouncement: (@Sendable (_ label: String) -> String)? = nil,
        empty: String? = nil
    ) {
        self.originAi = originAi
        self.originRule = originRule
        self.run = run
        self.runAction = runAction
        self.running = running
        self.done = done
        self.failed = failed
        self.dismiss = dismiss
        self.needsApproval = needsApproval
        self.risk = risk
        self.approvalTitle = approvalTitle
        self.approvalDescription = approvalDescription
        self.approve = approve
        self.cancel = cancel
        self.doneAnnouncement = doneAnnouncement
        self.empty = empty
    }
}
