/// Built-in strings of the duration input, from the `MonarchDurationInput` strings table.
enum MonarchDurationInputStrings {
    private static let table = "MonarchDurationInput"

    /// The placeholder of an empty field.
    static var prompt: String { MonarchStrings.string("monarch.durationInput.prompt", table: table) }

    /// Shown when the text cannot be read.
    static var invalid: String { MonarchStrings.string("monarch.durationInput.invalid", table: table) }

    /// Shown when the value is outside both bounds. The bounds are already formatted.
    static func between(_ minimum: String, _ maximum: String) -> String {
        MonarchStrings.string("monarch.durationInput.between", table: table, minimum, maximum)
    }

    /// Shown when the value is below the minimum and there is no maximum.
    static func atLeast(_ minimum: String) -> String {
        MonarchStrings.string("monarch.durationInput.atLeast", table: table, minimum)
    }

    /// Shown when the value is above the maximum.
    static func atMost(_ maximum: String) -> String {
        MonarchStrings.string("monarch.durationInput.atMost", table: table, maximum)
    }

    /// Spoken as the hint of a field that has an error.
    static func error(_ message: String) -> String {
        MonarchStrings.string("monarch.durationInput.error", table: table, message)
    }
}
