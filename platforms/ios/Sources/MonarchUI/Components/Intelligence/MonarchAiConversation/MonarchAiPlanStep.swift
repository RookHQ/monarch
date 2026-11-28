/// One step of a plan.
public struct MonarchAiPlanStep: Identifiable, Sendable, Hashable, Codable {
    /// The id of the step.
    public var id: String
    /// What the step does.
    public var label: String
    /// Where the step is.
    public var state: MonarchAiPlanStepState

    /// A step.
    ///
    /// - Parameters:
    ///   - id: The id of the step.
    ///   - label: What the step does.
    ///   - state: Where the step is.
    public init(id: String, label: String, state: MonarchAiPlanStepState = .todo) {
        self.id = id
        self.label = label
        self.state = state
    }
}
