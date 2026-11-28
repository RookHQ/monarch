import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchTabs``.
enum MonarchTabsTokens {
    /// The type role of a tab's label.
    static let textStyle: MonarchTextStyle = .label
    /// Padding before and after a tab's label.
    static let horizontalPadding: CGFloat = MonarchTokens.Space.space3
    /// The padding of a tab when the strip is tightened to fit.
    static let tightHorizontalPadding: CGFloat = MonarchTokens.Space.space1
    /// The gap between the label and the count.
    static let countSpacing: CGFloat = MonarchTokens.Space.space1_5
    /// The thickness of the line under the selected tab.
    static let indicatorHeight: CGFloat = MonarchTokens.Space.space0_5
    /// The thickness of the line under the whole strip.
    static let borderWidth: CGFloat = MonarchShapes.borderWidth
    /// The padding between the track and its pills.
    static let trackPadding: CGFloat = MonarchTokens.Space.space0_5
    /// The colour of the line under the selected tab.
    static var indicatorColor: MonarchColorRole { \.accentStrong }
    /// The colour of the line under the whole strip.
    static var borderColor: MonarchColorRole { \.borderDefault }
    /// The colour of the track behind the pills.
    static var trackColor: MonarchColorRole { \.bgMuted }
    /// The colours of the selected pill. The border keeps it visible without the fill.
    static var pillAppearance: MonarchSurfaceAppearance {
        MonarchSurfaceAppearance(background: \.bgSurface, foreground: \.textDefault, border: \.borderDefault)
    }

    /// The shape of the track and of a pill.
    static var shape: RoundedRectangle { MonarchShapes.control }

    /// The gap between two tabs.
    static func spacing(_ variant: MonarchTabsVariant) -> CGFloat {
        switch variant {
        case .line: MonarchTokens.Space.space1
        case .pill: MonarchTokens.Space.space0_5
        }
    }

    /// The least height of a tab's visible label area.
    static func minHeight(_ variant: MonarchTabsVariant) -> CGFloat {
        switch variant {
        case .line: MonarchTokens.Control.md
        case .pill: MonarchTokens.Control.md - MonarchTokens.Space.space1_5
        }
    }

    /// The colour of a tab's label.
    static func labelColor(isSelected: Bool, isEnabled: Bool) -> MonarchColorRole {
        guard isEnabled else { return \.textDisabled }
        return isSelected ? \.textDefault : \.textMuted
    }

    /// Whether a count is shown: it is not for `nil` or an empty string.
    static func showsCount(_ count: String?) -> Bool {
        !(count ?? "").isEmpty
    }

    /// What VoiceOver reads as the name of a tab: the label, then the count.
    static func accessibilityLabel(title: String, count: String?) -> String {
        guard let count, showsCount(count) else { return title }
        return "\(title), \(count)"
    }
}
