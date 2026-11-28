import Foundation
import MonarchTokens
import SwiftUI

/// A band with its computed upper bound.
struct MonarchAiScoreRange: Equatable {
    /// The band.
    let band: MonarchAiScoreBand
    /// The highest score in the band: the next band's minimum minus one, or 100.
    let maximum: Double
}

/// Which way a factor moved the score.
enum MonarchAiScoreDirection: Equatable {
    /// It raised the score.
    case up
    /// It lowered the score.
    case down
    /// It did not change the score.
    case none
}

/// Component tokens and logic for ``MonarchAiScore``.
enum MonarchAiScoreTokens {
    /// The top of the scale.
    static let scaleMax: Double = 100
    /// The gap between the sections.
    static let sectionSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between items in a row.
    static let itemSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the lines of a factor or a legend row.
    static let lineSpacing: CGFloat = MonarchTokens.Space.space1
    /// The height of the bar track.
    static let barHeight: CGFloat = MonarchTokens.Space.space2
    /// The width of a band mark on the bar.
    static let tickWidth: CGFloat = MonarchTokens.Space.space0_5
    /// The height of a factor's impact bar.
    static let impactHeight: CGFloat = MonarchTokens.Space.space1
    /// The width of a factor's impact track, before Dynamic Type.
    static let impactWidth: CGFloat = MonarchTokens.Space.space16
    /// The size of a legend swatch.
    static let swatchSize: CGFloat = MonarchTokens.Space.space2_5
    /// The SF Symbol of a factor that raised the score.
    static let upSymbol = "arrow.up"
    /// The SF Symbol of a factor that lowered the score.
    static let downSymbol = "arrow.down"
    /// The colour role of the label and the numbers.
    static var text: MonarchColorRole { \.textDefault }
    /// The colour role of secondary text.
    static var muted: MonarchColorRole { \.textMuted }
    /// The colour role of the bar and impact tracks.
    static var track: MonarchColorRole { \.bgMuted }
    /// The colour role of the band marks on the bar.
    static var tick: MonarchColorRole { \.bgSurface }

    /// The score limited to 0 through 100, or `nil` when there is none.
    static func clamped(_ value: Double?) -> Double? {
        guard let value, value.isFinite else { return nil }
        return min(scaleMax, max(0, value))
    }

    /// The bands sorted by minimum, each with its upper bound.
    static func ranges(_ bands: [MonarchAiScoreBand]) -> [MonarchAiScoreRange] {
        let sorted = bands.sorted { $0.minimum < $1.minimum }
        return sorted.enumerated().map { index, band in
            let next = index + 1 < sorted.count ? sorted[index + 1] : nil
            return MonarchAiScoreRange(
                band: band,
                maximum: next.map { max(band.minimum, $0.minimum - 1) } ?? scaleMax
            )
        }
    }

    /// The band a score falls in: the one with the highest minimum at or
    /// below it, or the lowest band.
    static func band(for value: Double, in bands: [MonarchAiScoreBand]) -> MonarchAiScoreBand? {
        let sorted = ranges(bands).map(\.band)
        return sorted.last { value >= $0.minimum } ?? sorted.first
    }

    /// The tone of the ring or bar: the band's, or neutral without a score.
    static func tone(for value: Double?, in bands: [MonarchAiScoreBand]) -> MonarchTone {
        guard let value else { return .neutral }
        return band(for: value, in: bands)?.tone ?? .neutral
    }

    /// The colour role of the bar's fill and a legend swatch for a tone.
    static func fill(_ tone: MonarchTone) -> MonarchColorRole {
        MonarchTonePalette.palette(for: tone).solid
    }

    /// A whole number in a locale.
    static func number(_ value: Double, locale: Locale) -> String {
        value.rounded().formatted(.number.precision(.fractionLength(0)).locale(locale))
    }

    /// A signed impact: "+12", "-8", "0", with up to one decimal.
    static func signed(_ impact: Double, locale: Locale) -> String {
        impact.formatted(
            .number.precision(.fractionLength(0...1)).sign(strategy: .always(includingZero: false)).locale(locale)
        )
    }

    /// Which way a factor moved the score.
    static func direction(_ impact: Double) -> MonarchAiScoreDirection {
        impact > 0 ? .up : (impact < 0 ? .down : .none)
    }

    /// The colour role of a factor's impact for its direction.
    static func impactRole(_ direction: MonarchAiScoreDirection) -> MonarchColorRole {
        switch direction {
        case .up: \.successStrong
        case .down: \.dangerStrong
        case .none: \.textMuted
        }
    }

    /// The share of the impact track a factor fills: its size against the largest.
    static func impactFraction(_ impact: Double, factors: [MonarchAiScoreFactor]) -> Double {
        let largest = max(1, factors.map { abs($0.impact) }.max() ?? 1)
        return min(1, abs(impact) / largest)
    }

    /// The share of the bar a score fills.
    static func fraction(_ value: Double?) -> Double {
        (value ?? 0) / scaleMax
    }

    /// The band marks on the bar, as shares of its width: every band minimum
    /// above the first.
    static func ticks(_ bands: [MonarchAiScoreBand]) -> [Double] {
        ranges(bands).dropFirst().map { min(1, max(0, $0.band.minimum / scaleMax)) }
    }

    /// What VoiceOver reads for the score: "72 out of 100", or "No score yet".
    static func valueText(_ value: Double?, locale: Locale, labels: MonarchAiScoreLabels) -> String {
        guard let value else { return labels.unavailable ?? MonarchAiScoreStrings.unavailable }
        let shown = number(value, locale: locale)
        let maximum = number(scaleMax, locale: locale)
        return labels.valueText?(shown, maximum) ?? MonarchAiScoreStrings.valueText(shown, max: maximum)
    }

    /// The spoken value of the bar: the score and the band, "72 out of 100, High".
    static func barValue(
        _ value: Double?,
        band: MonarchAiScoreBand?,
        locale: Locale,
        labels: MonarchAiScoreLabels
    ) -> String {
        let text = valueText(value, locale: locale, labels: labels)
        guard value != nil, let band else { return text }
        return "\(text), \(band.label)"
    }

    /// The spoken effect of a factor.
    static func impactText(_ impact: Double, locale: Locale, labels: MonarchAiScoreLabels) -> String {
        let direction = direction(impact)
        guard direction != .none else { return labels.noImpact ?? MonarchAiScoreStrings.noImpact }
        let amount = abs(impact).formatted(.number.precision(.fractionLength(0...1)).locale(locale))
        return labels.impact?(amount, direction == .up)
            ?? MonarchAiScoreStrings.impact(amount, raised: direction == .up)
    }

    /// The range of a band as text: "40–69".
    static func rangeText(_ range: MonarchAiScoreRange, locale: Locale) -> String {
        "\(number(range.band.minimum, locale: locale))–\(number(range.maximum, locale: locale))"
    }

    /// The ring size for a score size.
    static func ringSize(_ size: MonarchAiScoreSize) -> MonarchScoreRingSize {
        switch size {
        case .sm: .sm
        case .md: .md
        }
    }
}
