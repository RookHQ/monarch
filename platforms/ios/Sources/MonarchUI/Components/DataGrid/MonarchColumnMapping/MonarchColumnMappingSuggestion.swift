/// A field suggested for a source column by
/// ``MonarchColumnMapping/suggest(_:fields:threshold:)``.
public struct MonarchColumnMappingSuggestion: Sendable, Hashable {
    /// The id of the suggested field.
    public let fieldID: String
    /// Whether it is an exact or a close match.
    public let confidence: MonarchColumnMappingConfidence
    /// The similarity, from 0 to 1, rounded to two decimals.
    public let score: Double

    /// A suggestion.
    ///
    /// - Parameters:
    ///   - fieldID: The id of the suggested field.
    ///   - confidence: Whether it is an exact or a close match.
    ///   - score: The similarity, from 0 to 1.
    public init(fieldID: String, confidence: MonarchColumnMappingConfidence, score: Double) {
        self.fieldID = fieldID
        self.confidence = confidence
        self.score = score
    }
}
