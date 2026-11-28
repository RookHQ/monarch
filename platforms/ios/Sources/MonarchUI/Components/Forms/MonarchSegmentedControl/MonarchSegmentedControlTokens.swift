import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchSegmentedControl``.
enum MonarchSegmentedControlTokens {
    /// The padding between the track and its segments.
    static let trackPadding: CGFloat = MonarchTokens.Space.space0_5
    /// The gap between two segments.
    static let spacing: CGFloat = MonarchTokens.Space.space0_5
    /// The gap between the icon, the label and the count.
    static let contentSpacing: CGFloat = MonarchTokens.Space.space1_5
    /// The colour of the track.
    static var trackColor: MonarchColorRole { \.bgMuted }
    /// The surface of the selected segment: raised, with a hairline.
    static func pillAppearance(isDark: Bool) -> MonarchSurfaceAppearance {
        // In dark the pill is a step above the track (no shadow to lift it);
        // in light it is the surface colour, with a faint shadow.
        MonarchSurfaceAppearance(
            background: isDark ? \.bgEmphasis : \.bgSurface,
            foreground: \.textDefault,
            border: isDark ? \.borderDefault : \.borderSubtle
        )
    }
    /// The shadow under the selected segment: a faint one in light, none in
    /// dark, where the surface step and the hairline separate the pill.
    static func pillElevation(isDark: Bool) -> MonarchElevationLevel {
        isDark ? .none : .xs
    }
    /// The shape of the track.
    static var trackShape: RoundedRectangle { MonarchShapes.control }
    /// The shape of a segment: a smaller radius than the track.
    static var segmentShape: RoundedRectangle {
        RoundedRectangle(cornerRadius: MonarchTokens.Radius.sm, style: .continuous)
    }

    /// The visible height of the track: 32, 36 or 40 points.
    static func trackHeight(_ size: MonarchButtonSize) -> CGFloat {
        switch size {
        case .xs: MonarchTokens.Control.xs
        case .sm: MonarchTokens.Control.sm
        case .md: MonarchTokens.Control.md
        case .lg: MonarchTokens.Control.lg
        }
    }

    /// The visible height of a segment: the track less its padding.
    static func segmentHeight(_ size: MonarchButtonSize) -> CGFloat {
        trackHeight(size) - trackPadding * 2
    }

    /// Padding before and after the content of a segment.
    static func horizontalPadding(_ size: MonarchButtonSize) -> CGFloat {
        switch size {
        case .xs: MonarchTokens.Space.space2
        case .sm: MonarchTokens.Space.space2_5
        case .md: MonarchTokens.Space.space3
        case .lg: MonarchTokens.Space.space4
        }
    }

    /// The type role of a segment: 12 points small, 14 otherwise, medium.
    static func textStyle(_ size: MonarchButtonSize) -> MonarchTextStyle {
        size == .sm || size == .xs ? .caption : .label
    }

    /// The weight of a segment's label.
    static let fontWeight: Font.Weight = .medium

    /// The colour of a segment's content.
    static func contentColor(isSelected: Bool, isEnabled: Bool) -> MonarchColorRole {
        guard isEnabled else { return \.textDisabled }
        return isSelected ? \.textDefault : \.textMuted
    }

    /// Whether the icon is drawn alone: only when asked and when there is a symbol.
    static func showsIconOnly(isIconOnly: Bool, systemImage: String?) -> Bool {
        isIconOnly && systemImage?.isEmpty == false
    }

    /// Whether a count is shown: it is not for `nil` or an empty string.
    static func showsCount(_ count: String?) -> Bool {
        !(count ?? "").isEmpty
    }

    /// What VoiceOver reads as the name of a segment: the title, then the count.
    static func accessibilityLabel(title: String, count: String?) -> String {
        showsCount(count) ? "\(title), \(count ?? "")" : title
    }
}
