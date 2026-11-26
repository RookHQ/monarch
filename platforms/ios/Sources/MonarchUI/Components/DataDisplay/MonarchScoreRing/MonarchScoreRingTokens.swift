import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and rules for ``MonarchScoreRing``.
enum MonarchScoreRingTokens {
    /// The gap between the ring, the label and the description.
    static let spacing: CGFloat = MonarchTokens.Space.space2
    /// The width of the ring as a part of its diameter.
    static let lineWidthRatio: CGFloat = 0.1
    /// The thinnest the ring may be.
    static let minimumLineWidth: CGFloat = MonarchTokens.Space.space1
    /// How far the number may shrink to stay inside the ring.
    static let valueMinimumScale: CGFloat = 0.5
    /// Where the arc starts: the top of the ring.
    static let startAngle = Angle.degrees(-90)
    /// The top of the scale when the app gives none that is usable.
    static let defaultMax: Double = 100
    /// What the centre shows when there is no score. Decorative.
    static let noValueGlyph = "–"
    /// The track role.
    static var track: MonarchColorRole { \.bgMuted }
    /// The number role.
    static var value: MonarchColorRole { \.textDefault }
    /// The role of "/ 100" and the description.
    static var muted: MonarchColorRole { \.textMuted }

    /// The diameter of a size before Dynamic Type is applied.
    static func diameter(_ size: MonarchScoreRingSize) -> CGFloat {
        switch size {
        case .sm: MonarchTokens.Space.space12
        case .md: MonarchTokens.Space.space20
        case .lg: MonarchTokens.Space.space24 + MonarchTokens.Space.space6
        }
    }

    /// The width of the ring for a diameter: a tenth, at least 4 points.
    static func lineWidth(diameter: CGFloat) -> CGFloat {
        max(minimumLineWidth, (diameter * lineWidthRatio).rounded())
    }

    /// The type role of the number.
    static func valueStyle(_ size: MonarchScoreRingSize) -> MonarchTextStyle {
        switch size {
        case .sm: .label
        case .md: .title
        case .lg: .display
        }
    }

    /// Whether "/ 100" is shown: as asked, or by default on the md and lg
    /// sizes, and never without a score.
    static func showsMax(_ showsMax: Bool?, size: MonarchScoreRingSize, hasValue: Bool) -> Bool {
        hasValue && (showsMax ?? (size != .sm))
    }

    /// A usable top of the scale.
    static func safeMax(_ max: Double) -> Double {
        max > 0 && max.isFinite ? max : defaultMax
    }

    /// The score limited to 0 through `max`, or `nil` for no score.
    static func clamped(_ value: Double?, max: Double) -> Double? {
        guard let value, value.isFinite else { return nil }
        return Swift.min(safeMax(max), Swift.max(0, value))
    }

    /// The tone of a score: the band with the highest minimum the score
    /// reaches, or neutral when no band applies.
    static func tone(for value: Double, thresholds: [MonarchScoreRingThreshold]) -> MonarchTone {
        thresholds.sorted { $0.minimum > $1.minimum }.first { value >= $0.minimum }?.tone ?? .neutral
    }

    /// The colour role of the arc.
    static func fill(color: MonarchScoreRingColor, value: Double?, thresholds: [MonarchScoreRingThreshold])
        -> MonarchColorRole
    {
        let tone: MonarchTone
        switch color {
        case .accent: return \.accentSolid
        case .tone(let fixed): tone = fixed
        case .auto: tone = value.map { self.tone(for: $0, thresholds: thresholds) } ?? .neutral
        }
        return tone == .neutral ? \.textMuted : MonarchTonePalette.palette(for: tone).solid
    }

    /// The colour role of the number in the middle: the strong shade of the arc's tone, as the web, which reads
    /// better on the canvas than the solid shade.
    static func valueRole(color: MonarchScoreRingColor, value: Double?, thresholds: [MonarchScoreRingThreshold])
        -> MonarchColorRole
    {
        let tone: MonarchTone
        switch color {
        case .accent: return \.accentStrong
        case .tone(let fixed): tone = fixed
        case .auto: tone = value.map { self.tone(for: $0, thresholds: thresholds) } ?? .neutral
        }
        return tone == .neutral ? \.textMuted : MonarchTonePalette.palette(for: tone).strong
    }

    /// A number rounded to a whole number in a locale.
    static func formatted(_ value: Double, locale: Locale) -> String {
        value.formatted(.number.precision(.fractionLength(0)).locale(locale))
    }

    /// What VoiceOver reads as the value: "78 out of 100", or "No score yet".
    static func accessibilityValue(text: String?, max: String, labels: MonarchScoreRingLabels) -> String {
        guard let text else { return labels.noValue ?? MonarchScoreRingStrings.noValue }
        return labels.valueText?(text, max) ?? MonarchScoreRingStrings.valueText(text, max)
    }

    /// What VoiceOver reads as the name: the label, or "Score".
    static func accessibilityLabel(label: String?, labels: MonarchScoreRingLabels) -> String {
        if let label, !label.isEmpty { return label }
        return labels.name ?? MonarchScoreRingStrings.name
    }
}
