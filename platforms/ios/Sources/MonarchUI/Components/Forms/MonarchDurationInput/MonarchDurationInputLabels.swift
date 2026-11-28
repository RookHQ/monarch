/// Replacements for the built-in strings of ``MonarchDurationInput``.
///
/// Every property is `nil` by default, which keeps the built-in string.
public struct MonarchDurationInputLabels: Sendable {
    /// Shown when the text cannot be read. Built in: "Use a duration like 1h 30m, 90m or 1.5h."
    public var invalid: String?
    /// Shown when the value is outside the bounds. Receives the minimum and the maximum in minutes.
    /// Built in: "Enter between 15m and 2h.", "Enter at least 15m." or "Enter at most 2h."
    public var range: (@Sendable (Int, Int?) -> String)?
    /// The spoken value. Receives the minutes. Built in: the system's spelled-out duration, "1 hour, 30 minutes".
    public var valueText: (@Sendable (Int) -> String)?
    /// The spoken hint of a field with an error. Receives the message. Built in: "Error: <message>".
    public var errorHint: (@Sendable (String) -> String)?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        invalid: String? = nil,
        range: (@Sendable (Int, Int?) -> String)? = nil,
        valueText: (@Sendable (Int) -> String)? = nil,
        errorHint: (@Sendable (String) -> String)? = nil
    ) {
        self.invalid = invalid
        self.range = range
        self.valueText = valueText
        self.errorHint = errorHint
    }
}
