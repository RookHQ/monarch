/// Replacements for the built-in strings of ``MonarchScoreRing``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchScoreRingLabels: Sendable {
    /// The spoken name when there is no label. Built in: "Score".
    public var name: String?
    /// The spoken value. Receives the formatted score and maximum. Built in:
    /// "78 out of 100".
    public var valueText: (@Sendable (String, String) -> String)?
    /// The small text under the number. Receives the formatted maximum.
    /// Built in: "/ 100".
    public var ofMax: (@Sendable (String) -> String)?
    /// Spoken when there is no score. Built in: "No score yet".
    public var noValue: String?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        name: String? = nil,
        valueText: (@Sendable (String, String) -> String)? = nil,
        ofMax: (@Sendable (String) -> String)? = nil,
        noValue: String? = nil
    ) {
        self.name = name
        self.valueText = valueText
        self.ofMax = ofMax
        self.noValue = noValue
    }
}
