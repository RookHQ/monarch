/// Built-in strings of the number input, from the `MonarchNumberInput` strings table.
enum MonarchNumberInputStrings {
    private static let table = "MonarchNumberInput"

    /// Shown when the value is outside both bounds. The bounds are already formatted.
    static func range(minimum: String, maximum: String) -> String {
        MonarchStrings.string("monarch.numberInput.range", table: table, minimum, maximum)
    }

    /// Shown when the value is below the only bound.
    static func atLeast(_ minimum: String) -> String {
        MonarchStrings.string("monarch.numberInput.atLeast", table: table, minimum)
    }

    /// Shown when the value is above the only bound.
    static func atMost(_ maximum: String) -> String {
        MonarchStrings.string("monarch.numberInput.atMost", table: table, maximum)
    }

    /// Spoken as the hint of a number input that has an error.
    static func error(_ message: String) -> String {
        MonarchStrings.string("monarch.numberInput.error", table: table, message)
    }

    /// The spoken name of the button that steps up. Receives the field's label.
    static func increment(_ label: String) -> String {
        MonarchStrings.string("monarch.numberInput.increment", table: table, label)
    }

    /// The spoken name of the button that steps down. Receives the field's label.
    static func decrement(_ label: String) -> String {
        MonarchStrings.string("monarch.numberInput.decrement", table: table, label)
    }
}
