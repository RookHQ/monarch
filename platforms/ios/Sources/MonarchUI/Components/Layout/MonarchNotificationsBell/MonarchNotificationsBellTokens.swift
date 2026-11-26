import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchNotificationsBell``.
enum MonarchNotificationsBellTokens {
    /// The SF Symbol of the bell.
    static let symbol = "bell"
    /// The least width and height of the visible button.
    static let boxSize: CGFloat = MonarchTokens.Control.md
    /// Padding before and after the content of a row.
    static let rowHorizontalPadding: CGFloat = MonarchTokens.Space.space2_5
    /// Padding above and below the content of a row.
    static let rowVerticalPadding: CGFloat = MonarchTokens.Space.space1_5
    /// The gap between the bell, the title and the count in a row.
    static let rowSpacing: CGFloat = MonarchTokens.Space.space3
    /// The width of the ring that separates the count from the bell.
    static let badgeRing: CGFloat = MonarchTokens.Space.space0_5
    /// The part of the count's width that lies over the bell. The rest lies past its trailing edge.
    static let badgeOverlap: CGFloat = 0.25
    /// The largest count shown as a number. Above it the badge reads "99+".
    static let maximumCount = 99
    /// The type role of the title in a row.
    static let titleStyle: MonarchTextStyle = .label

    /// The shape of the pressed background.
    static var shape: RoundedRectangle { MonarchShapes.control }

    /// The colour roles for a state. There is no fill until the button is pressed.
    static func appearance(isPressed: Bool, isEnabled: Bool) -> MonarchSurfaceAppearance {
        guard isEnabled else {
            return MonarchSurfaceAppearance(background: nil, foreground: \.textDisabled, border: nil)
        }
        return isPressed
            ? MonarchSurfaceAppearance(background: \.bgMuted, foreground: \.textDefault, border: nil)
            : MonarchSurfaceAppearance(background: nil, foreground: \.textMuted, border: nil)
    }

    /// The text of the count badge, or `nil` when nothing is unread.
    static func badgeText(_ count: Int) -> String? {
        guard count > 0 else { return nil }
        if count > maximumCount {
            return MonarchNotificationsBellStrings.overflow(maximumCount.formatted())
        }
        return count.formatted()
    }

    /// What VoiceOver reads as the value: the exact count, or nothing when nothing is unread.
    static func accessibilityValue(count: Int, override: String?) -> String {
        guard count > 0 else { return "" }
        return override ?? MonarchNotificationsBellStrings.unread(count.formatted())
    }
}
