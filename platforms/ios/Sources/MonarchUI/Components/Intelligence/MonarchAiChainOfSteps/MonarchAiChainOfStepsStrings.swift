/// Built-in strings of the chain of steps, from the `MonarchAiChainOfSteps` strings table.
enum MonarchAiChainOfStepsStrings {
    private static let table = "MonarchAiChainOfSteps"

    /// The spoken name of the list.
    static var label: String { MonarchStrings.string("monarch.aiChainOfSteps.label", table: table) }

    /// The name of a state.
    static func state(_ state: MonarchAiPlanStepState) -> String {
        MonarchStrings.string("monarch.aiChainOfSteps.state.\(state.rawValue)", table: table)
    }

    /// The summary of the folded list.
    static func summary(count: Int, failed: Int) -> String {
        switch (count == 1, failed > 0) {
        case (true, false): MonarchStrings.string("monarch.aiChainOfSteps.summary.one", table: table)
        case (true, true): MonarchStrings.string("monarch.aiChainOfSteps.summary.oneFailed", table: table)
        case (false, false): MonarchStrings.string("monarch.aiChainOfSteps.summary", table: table, count)
        case (false, true):
            MonarchStrings.string("monarch.aiChainOfSteps.summary.failed", table: table, count, failed)
        }
    }

    /// Announced when a step starts.
    static func started(_ label: String, position: Int, total: Int) -> String {
        MonarchStrings.string("monarch.aiChainOfSteps.started", table: table, position, total, label)
    }

    /// Announced when a step fails.
    static func failedStep(_ label: String) -> String {
        MonarchStrings.string("monarch.aiChainOfSteps.failedStep", table: table, label)
    }

    /// Announced once every step has finished.
    static func finished(count: Int, failed: Int) -> String {
        failed > 0
            ? MonarchStrings.string("monarch.aiChainOfSteps.finished.failed", table: table, count, failed)
            : MonarchStrings.string("monarch.aiChainOfSteps.finished", table: table, count)
    }
}
