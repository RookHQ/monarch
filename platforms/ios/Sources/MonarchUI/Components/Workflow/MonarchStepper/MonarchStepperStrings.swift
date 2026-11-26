/// Built-in strings of the stepper, from the `MonarchStepper` strings table.
enum MonarchStepperStrings {
    private static let table = "MonarchStepper"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.stepper." + key, table: table)
    }

    /// The spoken name of the stepper.
    static var progress: String { string("progress") }
    /// Read after a finished step.
    static var complete: String { string("complete") }
    /// Read after a step with an error.
    static var error: String { string("error") }
    /// Read after the current step.
    static var current: String { string("current") }
    /// Read after the current step while the stepper is busy.
    static var busy: String { string("busy") }
    /// Shown under an optional step.
    static var optional: String { string("optional") }

    /// The position of a step, such as "Step 2 of 5".
    static func stepOf(_ step: Int, _ total: Int) -> String {
        MonarchStrings.string("monarch.stepper.stepOf", table: table, step, total)
    }
}
