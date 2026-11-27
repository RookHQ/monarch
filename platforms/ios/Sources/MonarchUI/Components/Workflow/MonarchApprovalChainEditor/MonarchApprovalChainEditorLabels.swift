import Foundation

/// Replacements for the built-in strings of ``MonarchApprovalChainEditor``.
///
/// Every property is `nil` by default, which keeps the built-in string.
public struct MonarchApprovalChainEditorLabels: Sendable {
    /// The heading of the list. Built in: "Steps, in order".
    public var legend: String?
    /// The title of a step, from its position. Built in: "Step 1".
    public var step: (@Sendable (Int) -> String)?
    /// The label of the approver menu. Built in: "Approver".
    public var approver: String?
    /// The label of a target menu when the type has none. Built in: "Choose".
    public var target: String?
    /// The empty choice of a target menu. Built in: "Choose…".
    public var targetPlaceholder: String?
    /// The label of the name field. Built in: "Label (optional)".
    public var label: String?
    /// The placeholder of the name field. Built in: "e.g. Finance check".
    public var labelPlaceholder: String?
    /// The add button. Built in: "Add step".
    public var addStep: String?
    /// The remove button, from the position. Built in: "Remove step 1".
    public var removeStep: (@Sendable (Int) -> String)?
    /// The error under a target menu with nothing chosen. Built in: "Choose who decides this step."
    public var targetRequired: String?
    /// Shown instead of the add button at the most steps. Built in: "A chain can have up to 10 steps."
    public var maxReached: (@Sendable (Int) -> String)?
    /// The menu that moves a step, from the position. Built in: "Reorder step 1".
    public var reorder: (@Sendable (Int) -> String)?
    /// Moves a step one place earlier. Built in: "Move up".
    public var moveUp: String?
    /// Moves a step one place later. Built in: "Move down".
    public var moveDown: String?
    /// Announced after a move, from the new position. Built in: "Moved to position 2."
    public var moved: (@Sendable (Int) -> String)?

    /// Replacements for built-in strings. Pass only the ones to replace.
    ///
    /// - Parameters:
    ///   - legend: The heading of the list.
    ///   - step: The title of a step.
    ///   - approver: The label of the approver menu.
    ///   - target: The label of a target menu when the type has none.
    ///   - targetPlaceholder: The empty choice of a target menu.
    ///   - label: The label of the name field.
    ///   - labelPlaceholder: The placeholder of the name field.
    ///   - addStep: The add button.
    ///   - removeStep: The remove button.
    ///   - targetRequired: The error under a target menu with nothing chosen.
    ///   - maxReached: Shown instead of the add button at the most steps.
    ///   - reorder: The menu that moves a step.
    ///   - moveUp: Moves a step one place earlier.
    ///   - moveDown: Moves a step one place later.
    ///   - moved: Announced after a move.
    public init(
        legend: String? = nil,
        step: (@Sendable (Int) -> String)? = nil,
        approver: String? = nil,
        target: String? = nil,
        targetPlaceholder: String? = nil,
        label: String? = nil,
        labelPlaceholder: String? = nil,
        addStep: String? = nil,
        removeStep: (@Sendable (Int) -> String)? = nil,
        targetRequired: String? = nil,
        maxReached: (@Sendable (Int) -> String)? = nil,
        reorder: (@Sendable (Int) -> String)? = nil,
        moveUp: String? = nil,
        moveDown: String? = nil,
        moved: (@Sendable (Int) -> String)? = nil
    ) {
        self.legend = legend
        self.step = step
        self.approver = approver
        self.target = target
        self.targetPlaceholder = targetPlaceholder
        self.label = label
        self.labelPlaceholder = labelPlaceholder
        self.addStep = addStep
        self.removeStep = removeStep
        self.targetRequired = targetRequired
        self.maxReached = maxReached
        self.reorder = reorder
        self.moveUp = moveUp
        self.moveDown = moveDown
        self.moved = moved
    }
}
