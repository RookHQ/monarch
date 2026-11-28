/// Replacements for the built-in strings of ``MonarchDateRangePicker``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchDateRangePickerLabels: Sendable {
    /// The label of the first date. Built in: "Start".
    public var start: String?
    /// The label of the last date. Built in: "End".
    public var end: String?
    /// The button shown when no range is chosen. Built in: "Choose dates".
    public var chooseDates: String?
    /// The button that clears the range. Built in: "Clear dates".
    public var clear: String?
    /// The button that opens the quick picks. Built in: "Quick picks".
    public var presets: String?
    /// The error when the first or last day is one the app disabled. Built in:
    /// "That date is not available."
    public var unavailable: String?
    /// The number of days in the range. Receives the count. Built in: "1 day", "7 days".
    public var dayCount: (@Sendable (Int) -> String)?

    /// Replacements for built-in strings. Every parameter is optional.
    ///
    /// - Parameters:
    ///   - start: The label of the first date.
    ///   - end: The label of the last date.
    ///   - chooseDates: The button shown when no range is chosen.
    ///   - clear: The button that clears the range.
    ///   - presets: The button that opens the quick picks.
    ///   - unavailable: The error for a disabled first or last day.
    ///   - dayCount: The number of days in the range, from the count.
    public init(
        start: String? = nil,
        end: String? = nil,
        chooseDates: String? = nil,
        clear: String? = nil,
        presets: String? = nil,
        unavailable: String? = nil,
        dayCount: (@Sendable (Int) -> String)? = nil
    ) {
        self.start = start
        self.end = end
        self.chooseDates = chooseDates
        self.clear = clear
        self.presets = presets
        self.unavailable = unavailable
        self.dayCount = dayCount
    }
}
