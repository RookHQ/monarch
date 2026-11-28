/// A confidence band.
public enum MonarchAiConfidenceLevel: Sendable, Hashable, CaseIterable {
    /// Below the low threshold. Read as "Needs check".
    case low
    /// Between the thresholds.
    case medium
    /// At or above the high threshold.
    case high

    /// The band of a confidence value: below `lowThreshold` is low, at or
    /// above `highThreshold` is high, anything between is medium.
    ///
    /// - Parameters:
    ///   - value: The confidence from 0 to 1. Values outside the range are clamped; not-a-number counts as 0.
    ///   - lowThreshold: The edge under which confidence is low.
    ///   - highThreshold: The edge from which confidence is high.
    /// - Returns: The band.
    public static func level(
        for value: Double,
        lowThreshold: Double = 0.6,
        highThreshold: Double = 0.85
    ) -> MonarchAiConfidenceLevel {
        let clamped = MonarchAiConfidenceMeterTokens.clamped(value)
        if clamped < lowThreshold { return .low }
        if clamped >= highThreshold { return .high }
        return .medium
    }
}
