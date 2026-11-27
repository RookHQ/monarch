import Foundation

/// Replacements for the built-in strings of ``MonarchAiToolApproval``.
///
/// Every property is `nil` by default, which keeps the built-in string.
@_spi(Experimental) public struct MonarchAiToolApprovalLabels: Sendable {
    /// The small label above the title. Built in: "Approval needed".
    public var eyebrow: String?
    /// The risk badge text. Built in: "Low risk", "Medium risk", "High risk", "Can’t be undone".
    public var risk: (@Sendable (MonarchAiToolCallRisk) -> String)?
    /// The consequence line when no `consequence` is passed; `nil` shows none.
    /// Built in: "This can’t be undone once it runs." for destructive calls only.
    public var consequence: (@Sendable (MonarchAiToolCallRisk) -> String?)?
    /// The heading of the arguments. Built in: "Details".
    public var arguments: String?
    /// Shown when the call has no arguments. Built in: "No arguments."
    public var noArguments: String?
    /// The approve button. Built in: "Approve".
    public var approve: String?
    /// The deny button. Built in: "Deny".
    public var deny: String?
    /// Opens the editor of the arguments. Built in: "Edit arguments".
    public var editArguments: String?
    /// Closes the editor and drops the edits. Built in: "Discard edits".
    public var discardEdits: String?
    /// The label of the editor. Built in: "Arguments (JSON)".
    public var argumentsJSON: String?
    /// The error when the edited arguments are not valid JSON. Built in: "Fix the JSON before approving."
    public var invalidJSON: String?
    /// Opens the reason field. Built in: "Add a reason".
    public var addReason: String?
    /// The label of the reason field. Built in: "Reason (optional)".
    public var reason: String?
    /// The always-allow checkbox. Built in: "Always allow this tool".
    public var alwaysAllow: String?
    /// Shown once the call is approved. Built in: "Approved".
    public var approved: String?
    /// Shown once the call is denied. Built in: "Denied".
    public var denied: String?
    /// Shown when the reply stopped before anyone decided. Built in: "Cancelled".
    public var cancelled: String?
    /// Announced when the card appears, from the title. Built in: "Approval needed: Send email".
    public var announce: (@Sendable (String) -> String)?

    /// Replacements for built-in strings. Pass only the ones to replace.
    ///
    /// - Parameters:
    ///   - eyebrow: The small label above the title.
    ///   - risk: The risk badge text.
    ///   - consequence: The consequence line when no `consequence` is passed.
    ///   - arguments: The heading of the arguments.
    ///   - noArguments: Shown when the call has no arguments.
    ///   - approve: The approve button.
    ///   - deny: The deny button.
    ///   - editArguments: Opens the editor of the arguments.
    ///   - discardEdits: Closes the editor and drops the edits.
    ///   - argumentsJSON: The label of the editor.
    ///   - invalidJSON: The error when the edited arguments are not valid JSON.
    ///   - addReason: Opens the reason field.
    ///   - reason: The label of the reason field.
    ///   - alwaysAllow: The always-allow checkbox.
    ///   - approved: Shown once the call is approved.
    ///   - denied: Shown once the call is denied.
    ///   - cancelled: Shown when the reply stopped before anyone decided.
    ///   - announce: Announced when the card appears.
    public init(
        eyebrow: String? = nil,
        risk: (@Sendable (MonarchAiToolCallRisk) -> String)? = nil,
        consequence: (@Sendable (MonarchAiToolCallRisk) -> String?)? = nil,
        arguments: String? = nil,
        noArguments: String? = nil,
        approve: String? = nil,
        deny: String? = nil,
        editArguments: String? = nil,
        discardEdits: String? = nil,
        argumentsJSON: String? = nil,
        invalidJSON: String? = nil,
        addReason: String? = nil,
        reason: String? = nil,
        alwaysAllow: String? = nil,
        approved: String? = nil,
        denied: String? = nil,
        cancelled: String? = nil,
        announce: (@Sendable (String) -> String)? = nil
    ) {
        self.eyebrow = eyebrow
        self.risk = risk
        self.consequence = consequence
        self.arguments = arguments
        self.noArguments = noArguments
        self.approve = approve
        self.deny = deny
        self.editArguments = editArguments
        self.discardEdits = discardEdits
        self.argumentsJSON = argumentsJSON
        self.invalidJSON = invalidJSON
        self.addReason = addReason
        self.reason = reason
        self.alwaysAllow = alwaysAllow
        self.approved = approved
        self.denied = denied
        self.cancelled = cancelled
        self.announce = announce
    }
}
