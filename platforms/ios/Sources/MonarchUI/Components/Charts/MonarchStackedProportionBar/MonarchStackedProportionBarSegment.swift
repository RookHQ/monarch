/// One part of a ``MonarchStackedProportionBar``.
///
/// A segment is coloured from the chart palette, or by a status tone when
/// every segment of the bar is a state (operational, degraded, down). The
/// two never mix: if only some segments have a tone, the tones are ignored.
public struct MonarchStackedProportionBarSegment: Sendable, Hashable, Identifiable {
    /// The stable key of the part.
    public let key: String
    /// The visible name, already localised. Always shown, so colour never
    /// carries the meaning alone.
    public let label: String
    /// The amount, in the same unit as the other parts. Negative and
    /// non-finite values count as 0.
    public let value: Double
    /// The palette slot, or `nil` for the slot of the segment's position.
    public let color: MonarchChartColor?
    /// The status tone, in status mode.
    public let tone: MonarchTone?

    /// The key.
    public var id: String { key }

    /// A part coloured from the chart palette.
    ///
    /// - Parameters:
    ///   - key: The stable key of the part.
    ///   - label: The visible name, already localised.
    ///   - value: The amount.
    ///   - color: The palette slot, or `nil` for the slot of the segment's position.
    public init(_ key: String, label: String, value: Double, color: MonarchChartColor? = nil) {
        self.key = key
        self.label = label
        self.value = value
        self.color = color
        self.tone = nil
    }

    /// A part that means a status, coloured by its tone. Give every segment
    /// of the bar a tone, or none.
    ///
    /// - Parameters:
    ///   - key: The stable key of the part.
    ///   - label: The visible name, already localised.
    ///   - value: The amount.
    ///   - tone: The status tone. `neutral` is for no data or paused.
    public init(_ key: String, label: String, value: Double, tone: MonarchTone) {
        self.key = key
        self.label = label
        self.value = value
        self.color = nil
        self.tone = tone
    }
}
