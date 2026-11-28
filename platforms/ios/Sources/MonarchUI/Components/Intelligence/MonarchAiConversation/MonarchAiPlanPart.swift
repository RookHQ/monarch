/// The steps the assistant intends to take.
public struct MonarchAiPlanPart: Sendable, Hashable, Codable {
    /// The steps, in order.
    public var steps: [MonarchAiPlanStep]

    /// A plan.
    ///
    /// - Parameter steps: The steps, in order.
    public init(steps: [MonarchAiPlanStep]) {
        self.steps = steps
    }
}
