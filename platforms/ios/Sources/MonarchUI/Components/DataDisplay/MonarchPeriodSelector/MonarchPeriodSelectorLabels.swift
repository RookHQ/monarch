/// Replacements for the built-in strings of ``MonarchPeriodSelector``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchPeriodSelectorLabels: Sendable {
    /// The names of presets, merged per preset. Built in: "Today", "Last 7 days", "This quarter", "Custom" and so on.
    public var presets: [MonarchPeriodPreset: String]
    /// The label of the custom date range. Built in: "Custom period".
    public var customRange: String?
    /// Replacements for the strings of the custom ``MonarchDateRangePicker``.
    public var dateRange: MonarchDateRangePickerLabels

    /// Replacements for built-in strings. Every parameter is optional.
    ///
    /// - Parameters:
    ///   - presets: The names of presets, merged per preset.
    ///   - customRange: The label of the custom date range.
    ///   - dateRange: Replacements for the strings of the custom date range picker.
    public init(
        presets: [MonarchPeriodPreset: String] = [:],
        customRange: String? = nil,
        dateRange: MonarchDateRangePickerLabels = MonarchDateRangePickerLabels()
    ) {
        self.presets = presets
        self.customRange = customRange
        self.dateRange = dateRange
    }
}
