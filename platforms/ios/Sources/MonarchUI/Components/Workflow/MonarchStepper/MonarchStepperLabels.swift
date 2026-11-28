/// Replacements for the built-in strings of ``MonarchStepper``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchStepperLabels: Sendable {
    /// The spoken name of the stepper. Built in: "Progress".
    public var progress: String?
    /// The position of a step, shown in the compact form and read before
    /// each step. Receives the number of the step, from 1, and the number
    /// of steps. Built in: "Step 2 of 5".
    public var stepOf: (@Sendable (Int, Int) -> String)?
    /// Read after a finished step. Built in: "completed".
    public var complete: String?
    /// Read after a step with an error. Built in: "needs attention".
    public var error: String?
    /// Read after the current step. Built in: "current step".
    public var current: String?
    /// Shown under an optional step. Built in: "Optional".
    public var optional: String?
    /// Read after the current step while the stepper is busy. Built in: "in progress".
    public var busy: String?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        progress: String? = nil,
        stepOf: (@Sendable (Int, Int) -> String)? = nil,
        complete: String? = nil,
        error: String? = nil,
        current: String? = nil,
        optional: String? = nil,
        busy: String? = nil
    ) {
        self.progress = progress
        self.stepOf = stepOf
        self.complete = complete
        self.error = error
        self.current = current
        self.optional = optional
        self.busy = busy
    }
}
