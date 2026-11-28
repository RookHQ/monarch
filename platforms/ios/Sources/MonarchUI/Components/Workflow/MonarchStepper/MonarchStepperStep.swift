/// One step of a ``MonarchStepper``.
public struct MonarchStepperStep: Identifiable, Sendable, Hashable {
    /// A stable identifier, unique among the steps.
    public let id: String
    /// The short name of the step, such as "Details".
    public var label: String
    /// Secondary text under the name.
    public var description: String?
    /// Whether the step may be skipped. An "Optional" note is shown.
    public var isOptional: Bool
    /// Whether the step is finished although it comes after the current
    /// step. Steps before the current step are finished without it.
    public var isComplete: Bool
    /// Whether the step needs attention. It takes precedence over every
    /// other state.
    public var hasError: Bool
    /// Whether the step cannot be selected even when it has been reached.
    public var isDisabled: Bool

    /// A step.
    ///
    /// - Parameters:
    ///   - id: A stable identifier, unique among the steps.
    ///   - label: The short name of the step, already localised.
    ///   - description: Secondary text under the name.
    ///   - isOptional: Whether the step may be skipped.
    ///   - isComplete: Whether the step is finished out of order.
    ///   - hasError: Whether the step needs attention.
    ///   - isDisabled: Whether the step cannot be selected.
    public init(
        id: String,
        label: String,
        description: String? = nil,
        isOptional: Bool = false,
        isComplete: Bool = false,
        hasError: Bool = false,
        isDisabled: Bool = false
    ) {
        self.id = id
        self.label = label
        self.description = description
        self.isOptional = isOptional
        self.isComplete = isComplete
        self.hasError = hasError
        self.isDisabled = isDisabled
    }
}
