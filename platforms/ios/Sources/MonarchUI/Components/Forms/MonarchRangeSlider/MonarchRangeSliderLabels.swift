/// Replacements for the built-in strings of ``MonarchRangeSlider``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchRangeSliderLabels: Sendable {
    /// Added to the label to name the lower thumb. Built in: "Minimum".
    public var minimum: String?
    /// Added to the label to name the upper thumb. Built in: "Maximum".
    public var maximum: String?
    /// The shown range. Receives the formatted lower and upper values.
    /// Built in: "20 – 80".
    public var range: (@Sendable (String, String) -> String)?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        minimum: String? = nil,
        maximum: String? = nil,
        range: (@Sendable (String, String) -> String)? = nil
    ) {
        self.minimum = minimum
        self.maximum = maximum
        self.range = range
    }
}
