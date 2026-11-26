/// Replacements for the built-in strings of ``MonarchDecisionCard``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchDecisionCardLabels: Sendable {
    /// The spoken name of the group of choices. Built in: "Decision".
    public var choices: String?
    /// The label of the choice that clears the decision. Built in: "No decision".
    public var none: String?
    /// The label of the reason field when a reason is required. Built in: "Reason".
    public var reason: String?
    /// The label of the reason field when a reason is optional. Built in:
    /// "Reason (optional)".
    public var reasonOptional: String?
    /// The placeholder of the reason field. Built in: "Why? Others will see this."
    public var reasonPrompt: String?
    /// Shown when a required reason is missing. Built in: "Add a reason for
    /// this decision."
    public var reasonRequired: String?
    /// The characters left, shown near the limit. Receives the number left.
    /// Built in: "1 character left", "12 characters left" (English plural
    /// rule only).
    public var charactersLeft: (@Sendable (Int) -> String)?
    /// The title of the save button. Built in: "Save decision".
    public var save: String?
    /// Announced after saving. Built in: "Decision saved."
    public var saved: String?
    /// The whole line that says when and by whom the decision was made.
    /// Built in: "Decided 3 hours ago by Ana".
    public var decided: String?
    /// The read-only text when nothing has been decided. Built in: "No
    /// decision yet".
    public var undecided: String?
    /// Shown when `onDecide` throws an error that has no description. Built
    /// in: "That did not work. Try again."
    public var failed: String?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        choices: String? = nil,
        none: String? = nil,
        reason: String? = nil,
        reasonOptional: String? = nil,
        reasonPrompt: String? = nil,
        reasonRequired: String? = nil,
        charactersLeft: (@Sendable (Int) -> String)? = nil,
        save: String? = nil,
        saved: String? = nil,
        decided: String? = nil,
        undecided: String? = nil,
        failed: String? = nil
    ) {
        self.choices = choices
        self.none = none
        self.reason = reason
        self.reasonOptional = reasonOptional
        self.reasonPrompt = reasonPrompt
        self.reasonRequired = reasonRequired
        self.charactersLeft = charactersLeft
        self.save = save
        self.saved = saved
        self.decided = decided
        self.undecided = undecided
        self.failed = failed
    }
}
