/// A stretch of a gauge's scale with a meaning, such as "Healthy" from 0 to 70.
public struct MonarchGaugeBand: Sendable, Hashable {
    /// The stretch of the scale. A value at the upper bound belongs to the
    /// next band, except at the end of the scale.
    public let range: ClosedRange<Double>
    /// The meaning, which selects the colour.
    public let tone: MonarchTone
    /// The name of the band, shown under the value and added to the spoken value.
    public let label: String?

    /// A band.
    ///
    /// - Parameters:
    ///   - range: The stretch of the scale.
    ///   - tone: The meaning, which selects the colour.
    ///   - label: The name of the band, already localised. Give one, so the
    ///     meaning does not depend on the colour.
    public init(_ range: ClosedRange<Double>, tone: MonarchTone, label: String? = nil) {
        self.range = range
        self.tone = tone
        self.label = label
    }
}
