/// A field of ``MonarchAiStructuredAnswer``: which value of the answer it
/// shows, its label and how the value is shown.
public struct MonarchAiStructuredField: Identifiable, Sendable {
    /// The key of the value in the answer's data.
    public var key: String
    /// The visible label, already localised.
    public var label: String
    /// How the value is shown.
    public var type: MonarchAiStructuredFieldType
    /// How a ``MonarchAiStructuredFieldType/number`` value is written.
    public var format: MonarchMetricFormat
    /// The tone of every badge of a ``MonarchAiStructuredFieldType/badge`` field.
    public var tone: MonarchTone
    /// The tone of each badge from its value. Replaces `tone` when set.
    public var toneForValue: (@Sendable (String) -> MonarchTone)?
    /// Whether the field spans the full width in the grid layout and puts its
    /// label above the value in the list layout. `nil`: only `list` and
    /// `markdown` fields do.
    public var isWide: Bool?

    /// The key, which identifies the field.
    public var id: String { key }

    /// A field.
    ///
    /// - Parameters:
    ///   - key: The key of the value in the answer's data.
    ///   - label: The visible label, already localised.
    ///   - type: How the value is shown.
    ///   - format: How a number value is written.
    ///   - tone: The tone of every badge.
    ///   - toneForValue: The tone of each badge from its value, for example warning for "At risk".
    ///   - isWide: Whether the field spans the full width. `nil`: only `list` and `markdown` fields do.
    public init(
        _ key: String,
        label: String,
        type: MonarchAiStructuredFieldType = .text,
        format: MonarchMetricFormat = .number,
        tone: MonarchTone = .neutral,
        toneForValue: (@Sendable (String) -> MonarchTone)? = nil,
        isWide: Bool? = nil
    ) {
        self.key = key
        self.label = label
        self.type = type
        self.format = format
        self.tone = tone
        self.toneForValue = toneForValue
        self.isWide = isWide
    }
}
