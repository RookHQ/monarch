/// One input that moved an AI score: positive when it raised the score,
/// negative when it lowered it.
public struct MonarchAiScoreFactor: Sendable, Hashable {
    /// What the factor is, such as "Opened the last three emails". Already localised.
    public var label: String
    /// The points the factor added (positive) or removed (negative).
    public var impact: Double
    /// A short explanation under the label. Already localised.
    public var detail: String?

    /// A factor.
    ///
    /// - Parameters:
    ///   - label: What the factor is.
    ///   - impact: The points it added (positive) or removed (negative).
    ///   - detail: A short explanation under the label.
    public init(_ label: String, impact: Double, detail: String? = nil) {
        self.label = label
        self.impact = impact
        self.detail = detail
    }
}
