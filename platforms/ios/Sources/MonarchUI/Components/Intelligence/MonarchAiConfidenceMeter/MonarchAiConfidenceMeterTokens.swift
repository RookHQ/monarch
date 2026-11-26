import MonarchTokens
import SwiftUI

/// Component tokens and logic for ``MonarchAiConfidenceMeter``.
enum MonarchAiConfidenceMeterTokens {
    /// The gap between the icon, the label and the value of the badge.
    static let badgeSpacing: CGFloat = MonarchTokens.Space.space1
    /// Padding above and below the content of the badge.
    static let badgeVerticalPadding: CGFloat = MonarchTokens.Space.space0_5
    /// The gap between the label and the value in the header of the bar.
    static let headerSpacing: CGFloat = MonarchTokens.Space.space2
    /// The weight of the band label.
    static let fontWeight = MonarchTextStyleSpec.fontWeight(MonarchTokens.FontWeight.medium)
    /// The role of the track of the bar.
    static var track: MonarchColorRole { \.bgMuted }
    /// The role of the percentage and the explanation.
    static var secondary: MonarchColorRole { \.textMuted }

    /// Padding before and after the content of the badge.
    static func badgeHorizontalPadding(_ size: MonarchAiConfidenceMeterSize) -> CGFloat {
        switch size {
        case .sm: MonarchTokens.Space.space1_5
        case .md: MonarchTokens.Space.space2
        }
    }

    /// The gap between the header, the track and the explanation of the bar.
    static func barSpacing(_ size: MonarchAiConfidenceMeterSize) -> CGFloat {
        switch size {
        case .sm: MonarchTokens.Space.space1
        case .md: MonarchTokens.Space.space1_5
        }
    }

    /// The height of the track.
    static func trackHeight(_ size: MonarchAiConfidenceMeterSize) -> CGFloat {
        switch size {
        case .sm: MonarchTokens.Space.space1
        case .md: MonarchTokens.Space.space1_5
        }
    }

    /// The type role of the band label in the bar.
    static func barTextStyle(_ size: MonarchAiConfidenceMeterSize) -> MonarchTextStyle {
        switch size {
        case .sm: .caption
        case .md: .label
        }
    }

    /// A confidence limited to 0 through 1. Not-a-number and infinity count as 0.
    static func clamped(_ value: Double) -> Double {
        guard value.isFinite else { return 0 }
        return min(max(value, 0), 1)
    }

    /// The confidence as a whole percentage in the user's locale.
    static func percent(_ value: Double) -> String {
        clamped(value).formatted(.percent.precision(.fractionLength(0)))
    }

    /// Whether the percentage is shown: the app's choice, otherwise only in the bar.
    static func showsValue(_ showsValue: Bool?, variant: MonarchAiConfidenceMeterVariant) -> Bool {
        showsValue ?? (variant == .bar)
    }

    /// The SF Symbol of a band, so the bands differ by shape.
    static func symbol(_ level: MonarchAiConfidenceLevel) -> String {
        switch level {
        case .low: "exclamationmark.triangle.fill"
        case .medium: "circle.lefthalf.filled"
        case .high: "checkmark.circle.fill"
        }
    }

    /// The role of the icon, the label and the fill of the bar.
    static func foreground(_ level: MonarchAiConfidenceLevel) -> MonarchColorRole {
        switch level {
        case .low: \.confidenceLow
        case .medium: \.confidenceMedium
        case .high: \.confidenceHigh
        }
    }

    /// The colour roles of the badge.
    static func badgeAppearance(_ level: MonarchAiConfidenceLevel) -> MonarchSurfaceAppearance {
        switch level {
        case .low:
            MonarchSurfaceAppearance(background: \.dangerSubtle, foreground: \.confidenceLow, border: \.dangerBorder)
        case .medium:
            MonarchSurfaceAppearance(
                background: \.warningSubtle,
                foreground: \.confidenceMedium,
                border: \.warningBorder
            )
        case .high:
            MonarchSurfaceAppearance(
                background: \.successSubtle,
                foreground: \.confidenceHigh,
                border: \.successBorder
            )
        }
    }

    /// What VoiceOver reads as the value, such as "Needs check, 42% confidence".
    static func valueText(
        value: Double,
        level: MonarchAiConfidenceLevel,
        levelLabel: String? = nil,
        format: ((String, String) -> String)? = nil
    ) -> String {
        let label = levelLabel ?? MonarchAiConfidenceMeterStrings.label(level)
        let percent = percent(value)
        return format?(label, percent) ?? MonarchAiConfidenceMeterStrings.valueText(label, percent)
    }
}
