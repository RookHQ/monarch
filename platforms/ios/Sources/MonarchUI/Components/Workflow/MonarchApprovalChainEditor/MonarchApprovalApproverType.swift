/// A kind of approver a step of ``MonarchApprovalChainEditor`` can name,
/// such as "Reporting manager" or "Anyone with a role".
public struct MonarchApprovalApproverType: Identifiable, Sendable, Hashable {
    /// The stable value stored on the step.
    public let id: String
    /// The name in the approver menu, already localised.
    public let label: String
    /// When the type needs a choice (which role, which person), the options.
    /// A step of this type is invalid until one is chosen. `nil` needs no choice.
    public let targets: [MonarchComboboxOption]?
    /// The label of the target menu, such as "Role" or "Person". "Choose" by default.
    public let targetLabel: String?

    /// An approver type.
    ///
    /// - Parameters:
    ///   - id: The stable value stored on the step.
    ///   - label: The name in the approver menu.
    ///   - targets: The options to choose from, when the type needs a choice.
    ///   - targetLabel: The label of the target menu.
    public init(id: String, label: String, targets: [MonarchComboboxOption]? = nil, targetLabel: String? = nil) {
        self.id = id
        self.label = label
        self.targets = targets
        self.targetLabel = targetLabel
    }
}
