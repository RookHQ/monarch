/// Replacements for the built-in strings of ``MonarchAiChainOfSteps``.
///
/// Every property is `nil` by default, which keeps the built-in string.
public struct MonarchAiChainOfStepsLabels: Sendable {
    /// The spoken name of the list. Built in: "Steps".
    public var label: String?
    /// The name of a state, read after each step and shown for failed and skipped steps.
    /// Built in: "Not started", "In progress", "Done", "Skipped", "Failed".
    public var state: (@Sendable (MonarchAiPlanStepState) -> String)?
    /// The summary of the folded list, from the step count and the failed count. Built in: "4 steps, 1 failed".
    public var summary: (@Sendable (Int, Int) -> String)?
    /// Announced when a step starts, from its label, its position and the step count. Built in: "Step 2 of 4: Read the file".
    public var started: (@Sendable (String, Int, Int) -> String)?
    /// Announced when a step fails, from its label. Built in: "Step failed: Read the file".
    public var failedStep: (@Sendable (String) -> String)?
    /// Announced once every step has finished, from the step count and the failed count. Built in: "Finished 4 steps".
    public var finished: (@Sendable (Int, Int) -> String)?

    /// Replacements for built-in strings. Pass only the ones to replace.
    public init(
        label: String? = nil,
        state: (@Sendable (MonarchAiPlanStepState) -> String)? = nil,
        summary: (@Sendable (Int, Int) -> String)? = nil,
        started: (@Sendable (String, Int, Int) -> String)? = nil,
        failedStep: (@Sendable (String) -> String)? = nil,
        finished: (@Sendable (Int, Int) -> String)? = nil
    ) {
        self.label = label
        self.state = state
        self.summary = summary
        self.started = started
        self.failedStep = failedStep
        self.finished = finished
    }
}
