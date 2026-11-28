import Foundation
import MonarchTokens

/// Component tokens for ``MonarchAiThinkingIndicator``.
enum MonarchAiThinkingIndicatorTokens {
    /// The gap between the mark, the label and the elapsed time.
    static let spacing: CGFloat = MonarchTokens.Space.space2
    /// The resting colour of the label.
    static var label: MonarchColorRole { \.textMuted }
    /// The colour of the shimmer band and of the mark.
    static var accent: MonarchColorRole { \.aiStrong }
    /// The colour of the elapsed time.
    static var elapsed: MonarchColorRole { \.textSubtle }
    /// Half the width of the shimmer band, as a fraction of the label width.
    static let bandHalfWidth: Double = 0.25
    /// The lowest opacity of the pulsing mark.
    static let markMinimumOpacity: Double = 0.45
    /// Seconds before the elapsed time appears, by default.
    static let defaultElapsedDelay: TimeInterval = 3

    /// Whole seconds since `start`, never negative.
    static func elapsedSeconds(now: Date, start: Date) -> Int {
        max(0, Int(now.timeIntervalSince(start).rounded(.down)))
    }

    /// Whether the elapsed time is shown after `seconds`.
    static func showsElapsed(seconds: Int, after delay: TimeInterval) -> Bool {
        guard delay.isFinite else { return false }
        return Double(seconds) >= delay
    }

    /// The built-in elapsed text: "5s", "1m 5s", "2m" in English, in the locale's words.
    static func elapsedText(seconds: Int, locale: Locale) -> String {
        Duration.seconds(seconds)
            .formatted(.units(allowed: [.hours, .minutes, .seconds], width: .narrow).locale(locale))
    }

    /// The centre of the shimmer band at a time, from just before the
    /// leading edge (`-bandHalfWidth`) to just past the trailing edge.
    static func bandCentre(at time: TimeInterval) -> Double {
        let phase = time.truncatingRemainder(dividingBy: MonarchPulse.period) / MonarchPulse.period
        return -bandHalfWidth + phase * (1 + 2 * bandHalfWidth)
    }

    /// The three gradient locations of the band (start, centre, end) for a
    /// centre, clamped to 0...1 and in order.
    static func bandLocations(centre: Double) -> (start: Double, centre: Double, end: Double) {
        func clamp(_ value: Double) -> Double { min(max(value, 0), 1) }
        return (clamp(centre - bandHalfWidth), clamp(centre), clamp(centre + bandHalfWidth))
    }

    /// The opacity of the mark at a time: from `markMinimumOpacity` to 1 and
    /// back over one period. `nil` time (Reduce Motion) gives a still, full mark.
    static func markOpacity(at time: TimeInterval?) -> Double {
        guard let time else { return 1 }
        let wave = 1 - MonarchPulse.value(at: time)
        return markMinimumOpacity + (1 - markMinimumOpacity) * wave
    }
}
