import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchAiLauncher``.
enum MonarchAiLauncherTokens {
    /// Padding around the content.
    static let padding: CGFloat = MonarchTokens.Space.space2
    /// Padding before and after the content when the label is shown.
    static let labelHorizontalPadding: CGFloat = MonarchTokens.Space.space3
    /// The gap between the glyph and the label.
    static let spacing: CGFloat = MonarchTokens.Space.space2
    /// The width of the border.
    static let borderWidth: CGFloat = MonarchTokens.Space.px
    /// The diameter of the unread dot.
    static let dotSize: CGFloat = MonarchTokens.Space.space3
    /// The least width and height of the count.
    static let countMinSize: CGFloat = MonarchTokens.Space.space5
    /// Padding before and after the digits of the count.
    static let countHorizontalPadding: CGFloat = MonarchTokens.Space.space1
    /// The width of the ring around the dot and the count, in the surface colour.
    static let badgeBorderWidth: CGFloat = MonarchTokens.Space.space0_5
    /// How far the dot and the count stand out past the top trailing corner.
    static let badgeOutset: CGFloat = MonarchTokens.Space.space1
    /// The width of the working ring.
    static let ringWidth: CGFloat = MonarchTokens.Space.space0_5
    /// The gap between the button and the working ring.
    static let ringOutset: CGFloat = MonarchTokens.Space.space0_5
    /// The share of the outline the moving part of the working ring covers.
    static let ringArc: CGFloat = 0.5
    /// The time the working ring takes to go around once, in seconds.
    static let ringPeriod: TimeInterval = TimeInterval(MonarchTokens.Duration.shimmer) / 1000
    /// The largest count shown in digits.
    static let countLimit = 99
    /// The weight of the count.
    static let countWeight = MonarchTextStyleSpec.fontWeight(MonarchTokens.FontWeight.semibold)
    /// The SF Symbol of the open, floating launcher.
    static let closeSymbol = "xmark"

    /// The least width and height of the button, which is also its touch target.
    static func minSize(_ variant: MonarchAiLauncherVariant) -> CGFloat {
        switch variant {
        case .solid: max(MonarchTokens.Control.xl, MonarchAccessibility.minimumHitTarget)
        case .subtle: MonarchAccessibility.minimumHitTarget
        }
    }

    /// The size of the glyph.
    static func iconSize(_ variant: MonarchAiLauncherVariant) -> MonarchIconSize {
        switch variant {
        case .solid: .md
        case .subtle: .sm
        }
    }

    /// The shadow under the button. Only the floating launcher is raised.
    static func elevation(_ variant: MonarchAiLauncherVariant) -> MonarchElevationLevel {
        switch variant {
        case .solid: .md
        case .subtle: .none
        }
    }

    /// The corner radius. The floating launcher is fully round.
    static func cornerRadius(_ variant: MonarchAiLauncherVariant) -> CGFloat {
        switch variant {
        case .solid: MonarchTokens.Radius.full
        case .subtle: MonarchTokens.Radius.md
        }
    }

    /// Whether the label is shown: as asked, otherwise only for the subtle launcher.
    static func showsLabel(_ showsLabel: Bool?, variant: MonarchAiLauncherVariant) -> Bool {
        showsLabel ?? (variant == .subtle)
    }

    /// What the button shows as its glyph.
    enum Glyph: Equatable {
        /// The close mark of an open floating launcher.
        case close
        /// An SF Symbol the app chose.
        case symbol(String)
        /// The AI identity mark.
        case bot
    }

    /// The glyph for a state. An open floating launcher shows a close mark.
    static func glyph(isOpen: Bool, variant: MonarchAiLauncherVariant, systemImage: String?) -> Glyph {
        if isOpen && variant == .solid { return .close }
        if let systemImage { return .symbol(systemImage) }
        return .bot
    }

    /// The colour roles for a state. Pure, so it is tested without a view.
    static func appearance(
        variant: MonarchAiLauncherVariant,
        isOpen: Bool,
        isPressed: Bool,
        isEnabled: Bool
    ) -> MonarchSurfaceAppearance {
        switch variant {
        case .solid:
            guard isEnabled else {
                return MonarchSurfaceAppearance(background: \.bgMuted, foreground: \.textDisabled, border: nil)
            }
            return MonarchSurfaceAppearance(
                background: isPressed ? \.textDefault : \.bgInverse,
                foreground: \.textInverse,
                border: nil
            )
        case .subtle:
            guard isEnabled else {
                return MonarchSurfaceAppearance(
                    background: \.bgSubtle,
                    foreground: \.textDisabled,
                    border: \.borderSubtle
                )
            }
            return MonarchSurfaceAppearance(
                background: isPressed ? \.bgMuted : \.bgSurface,
                foreground: \.aiStrong,
                border: isOpen ? \.aiSolid : \.borderDefault
            )
        }
    }

    /// The digits of a count, or `nil` when nothing is shown.
    static func countText(_ count: Int) -> String? {
        guard count > 0 else { return nil }
        return count > countLimit ? "\(countLimit)+" : "\(count)"
    }

    /// What VoiceOver reads as the value: whether the assistant is open, then the badge.
    static func accessibilityValue(isOpen: Bool, openLabel: String, badgeLabel: String?) -> String {
        var parts: [String] = []
        if isOpen { parts.append(openLabel) }
        if let badgeLabel, !badgeLabel.isEmpty { parts.append(badgeLabel) }
        return parts.joined(separator: ", ")
    }

    /// The two parts of the working ring at a time, as shares of the outline.
    /// The moving part wraps around the end of the outline, so it may be in two pieces.
    static func ringSegments(phase: CGFloat) -> [ClosedRange<CGFloat>] {
        let start = phase - phase.rounded(.down)
        let end = start + ringArc
        if end <= 1 { return [start...end] }
        return [start...1, 0...(end - 1)]
    }
}
