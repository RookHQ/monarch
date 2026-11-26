/// A band of the score scale: scores at or above `minimum` take `tone`.
public struct MonarchScoreRingThreshold: Sendable, Hashable {
    /// The lowest score in the band.
    public let minimum: Double
    /// The tone of the band.
    public let tone: MonarchTone

    /// A band.
    ///
    /// - Parameters:
    ///   - minimum: The lowest score in the band.
    ///   - tone: The tone of the band.
    public init(minimum: Double, tone: MonarchTone) {
        self.minimum = minimum
        self.tone = tone
    }

    /// The bands on a 0 to 100 scale: 80 and above success, 60 info, 40
    /// warning, below 40 danger.
    public static let defaults: [MonarchScoreRingThreshold] = [
        MonarchScoreRingThreshold(minimum: 80, tone: .success),
        MonarchScoreRingThreshold(minimum: 60, tone: .info),
        MonarchScoreRingThreshold(minimum: 40, tone: .warning),
        MonarchScoreRingThreshold(minimum: -.infinity, tone: .danger),
    ]
}
