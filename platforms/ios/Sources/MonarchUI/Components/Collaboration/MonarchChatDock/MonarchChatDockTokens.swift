import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchChatDock``.
enum MonarchChatDockTokens {
    /// The side of the launcher.
    static let launcherSize: CGFloat = MonarchTokens.Space.space12
    /// The corner radius of the launcher.
    static let launcherRadius: CGFloat = MonarchTokens.Radius.lg
    /// The horizontal padding of the unread badge.
    static let badgeHorizontalPadding: CGFloat = MonarchTokens.Space.space1_5
    /// The smallest width of the unread badge.
    static let badgeMinimumSize: CGFloat = MonarchTokens.Space.space5
    /// How far the badge sits outside the launcher's corner.
    static let badgeOffset: CGFloat = MonarchTokens.Space.space1_5
    /// The highest count drawn; more shows "99+".
    static let maximumCount = 99
    /// The symbol of the launcher.
    static let symbol = "bubble.left"
    /// The elevation of the launcher.
    static let elevation: MonarchElevationLevel = .md
    /// The fill of the launcher.
    static var launcherFill: MonarchColorRole { \.bgInverse }
    /// The symbol colour of the launcher.
    static var launcherForeground: MonarchColorRole { \.textInverse }
    /// The fill of the unread badge.
    static var badgeFill: MonarchColorRole { \.dangerSolid }
    /// The text colour of the unread badge.
    static var badgeForeground: MonarchColorRole { \.dangerOnSolid }

    /// The text of the badge, or nil for none.
    static func badgeText(_ unread: Int) -> String? {
        guard unread > 0 else { return nil }
        return unread > maximumCount ? "\(maximumCount)+" : "\(unread)"
    }

    /// The spoken value of the launcher: "3 unread", or nothing.
    static func accessibilityValue(unread: Int, format: String?) -> String {
        guard unread > 0 else { return "" }
        return String(format: format ?? MonarchChatDockStrings.unread, unread)
    }
}
