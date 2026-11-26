/// Replacements for the built-in strings of ``MonarchAiPlan``.
///
/// Every property is `nil` by default, which keeps the built-in string.
@_spi(Experimental) public struct MonarchAiPlanLabels: Sendable {
    /// The title above the steps. Built in: "Proposed plan".
    public var title: String?
    /// The explanation while the plan waits. Built in: "The assistant will take these steps after you approve. Nothing runs until then."
    public var description: String?
    /// Added to the explanation while the plan can be edited. Built in: "You can edit, reorder, add or remove steps first."
    public var editHint: String?
    /// Shown and announced once approved. Built in: "Plan approved. The assistant is working through it."
    public var approvedNotice: String?
    /// Shown and announced once rejected. Built in: "Plan rejected. The assistant will not take these steps."
    public var rejectedNotice: String?
    /// The spoken name of a step's text, from its position. Built in: "Step 2".
    public var stepLabel: (@Sendable (Int) -> String)?
    /// The placeholder of an empty step. Built in: "Describe this step".
    public var stepPlaceholder: String?
    /// The name of an empty step in spoken names, from its position. Built in: "step 2".
    public var untitledStep: (@Sendable (Int) -> String)?
    /// The spoken name of a step's drag handle, from its label. Built in: "Reorder Draft the reply".
    public var reorder: (@Sendable (String) -> String)?
    /// Announced after a move, from the label, the new position and the count. Built in: "Draft the reply, position 2 of 4".
    public var moved: (@Sendable (String, Int, Int) -> String)?
    /// The spoken name of a step's remove button, from its label. Built in: "Remove Draft the reply".
    public var remove: (@Sendable (String) -> String)?
    /// Announced after a removal, from the label. Built in: "Removed Draft the reply".
    public var removed: (@Sendable (String) -> String)?
    /// The add button. Built in: "Add step".
    public var addStep: String?
    /// The approve button. Built in: "Approve plan".
    public var approve: String?
    /// The reject button. Built in: "Reject".
    public var reject: String?
    /// Shown while every step is empty. Built in: "Add at least one step to approve the plan."
    public var emptyPlan: String?

    /// Replacements for built-in strings. Pass only the ones to replace.
    public init(
        title: String? = nil,
        description: String? = nil,
        editHint: String? = nil,
        approvedNotice: String? = nil,
        rejectedNotice: String? = nil,
        stepLabel: (@Sendable (Int) -> String)? = nil,
        stepPlaceholder: String? = nil,
        untitledStep: (@Sendable (Int) -> String)? = nil,
        reorder: (@Sendable (String) -> String)? = nil,
        moved: (@Sendable (String, Int, Int) -> String)? = nil,
        remove: (@Sendable (String) -> String)? = nil,
        removed: (@Sendable (String) -> String)? = nil,
        addStep: String? = nil,
        approve: String? = nil,
        reject: String? = nil,
        emptyPlan: String? = nil
    ) {
        self.title = title
        self.description = description
        self.editHint = editHint
        self.approvedNotice = approvedNotice
        self.rejectedNotice = rejectedNotice
        self.stepLabel = stepLabel
        self.stepPlaceholder = stepPlaceholder
        self.untitledStep = untitledStep
        self.reorder = reorder
        self.moved = moved
        self.remove = remove
        self.removed = removed
        self.addStep = addStep
        self.approve = approve
        self.reject = reject
        self.emptyPlan = emptyPlan
    }
}
