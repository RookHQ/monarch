/// A named range of an AI score, from `minimum` up to the next band's
/// minimum, such as "High" from 70.
public struct MonarchAiScoreBand: Sendable, Hashable {
    /// The lowest score in the band, from 0 to 100.
    public var minimum: Double
    /// The name shown beside the score, such as "High". Already localised.
    public var label: String
    /// The colour of the ring or bar while the score is in the band. Always
    /// shown with the label and the tone's icon, never alone.
    public var tone: MonarchTone

    /// A band.
    ///
    /// - Parameters:
    ///   - minimum: The lowest score in the band, from 0 to 100.
    ///   - label: The name shown beside the score.
    ///   - tone: The colour of the ring or bar while the score is in the band.
    public init(minimum: Double, label: String, tone: MonarchTone = .neutral) {
        self.minimum = minimum
        self.label = label
        self.tone = tone
    }

    /// The built-in bands: Low below 40 (danger), Medium from 40 (warning),
    /// High from 70 (success), with localised names.
    public static var defaults: [MonarchAiScoreBand] {
        [
            MonarchAiScoreBand(minimum: 0, label: MonarchAiScoreStrings.low, tone: .danger),
            MonarchAiScoreBand(minimum: 40, label: MonarchAiScoreStrings.medium, tone: .warning),
            MonarchAiScoreBand(minimum: 70, label: MonarchAiScoreStrings.high, tone: .success),
        ]
    }
}
