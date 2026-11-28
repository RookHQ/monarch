import SwiftUI

/// The button that opens an app's notifications, with the unread count.
///
/// It is a standard `Button`. The app presents its notifications in `action`
/// (a sheet, a popover or a pushed screen) and supplies the count.
///
/// ```swift
/// MonarchNotificationsBell(unreadCount: 3) {
///     showsNotifications = true
/// }
/// ```
///
/// Use `variant: .row` for a full-width entry in a menu or a sidebar.
///
/// Accessibility: the button is named "Notifications" and the unread count is
/// its value ("3 unread"), which is the exact count even when the badge shows
/// "99+". The bell and the badge are decorative. The touch target is at least
/// 44 by 44 points, and the bell, the title and the count scale with Dynamic
/// Type.
public struct MonarchNotificationsBell: View {
    private let unreadCount: Int
    private let variant: MonarchNotificationsBellVariant
    private let title: String?
    private let unreadLabel: String?
    private let action: @MainActor () -> Void

    @Environment(\.monarchTheme) private var theme

    /// A notifications button.
    ///
    /// - Parameters:
    ///   - unreadCount: The number of unread notifications. Zero or less shows no badge.
    ///   - variant: An icon button or a full-width row.
    ///   - title: Replaces the built-in name "Notifications", which is also the visible title of a row.
    ///   - unreadLabel: Replaces the built-in spoken value "3 unread". Pass the whole phrase for the count.
    ///   - action: Opens the notifications.
    public init(
        unreadCount: Int = 0,
        variant: MonarchNotificationsBellVariant = .icon,
        title: String? = nil,
        unreadLabel: String? = nil,
        action: @escaping @MainActor () -> Void
    ) {
        self.unreadCount = unreadCount
        self.variant = variant
        self.title = title
        self.unreadLabel = unreadLabel
        self.action = action
    }

    /// The content of the view.
    public var body: some View {
        let name = title ?? MonarchNotificationsBellStrings.title
        Button(action: action) {
            switch variant {
            case .icon: icon
            case .row: row(name)
            }
        }
        .buttonStyle(MonarchNotificationsBellButtonStyle())
        .accessibilityLabel(name)
        .accessibilityValue(
            MonarchNotificationsBellTokens.accessibilityValue(count: unreadCount, override: unreadLabel)
        )
    }

    private var icon: some View {
        bell
            .frame(
                minWidth: MonarchNotificationsBellTokens.boxSize,
                minHeight: MonarchNotificationsBellTokens.boxSize
            )
    }

    @ViewBuilder private var bell: some View {
        let symbol = MonarchIcon(systemName: MonarchNotificationsBellTokens.symbol)
        if let text = MonarchNotificationsBellTokens.badgeText(unreadCount) {
            symbol.overlay(alignment: .topTrailing) {
                badge(text)
                    .padding(MonarchNotificationsBellTokens.badgeRing)
                    .background(theme.colors.bgSurface, in: Capsule())
                    .fixedSize()
                    // The middle of the badge sits on the top edge of the bell and most of
                    // its width lies past the trailing edge, so a wide count grows outward.
                    .alignmentGuide(.top) { $0[VerticalAlignment.center] }
                    .alignmentGuide(.trailing) { dimensions in
                        let leading = dimensions[HorizontalAlignment.leading]
                        let trailing = dimensions[HorizontalAlignment.trailing]
                        return leading + (trailing - leading) * MonarchNotificationsBellTokens.badgeOverlap
                    }
            }
        } else {
            symbol
        }
    }

    private func row(_ name: String) -> some View {
        HStack(spacing: MonarchNotificationsBellTokens.rowSpacing) {
            MonarchIcon(systemName: MonarchNotificationsBellTokens.symbol)
            Text(name)
                .monarchTextStyle(MonarchNotificationsBellTokens.titleStyle)
                .multilineTextAlignment(.leading)
                .frame(maxWidth: .infinity, alignment: .leading)
            if let text = MonarchNotificationsBellTokens.badgeText(unreadCount) {
                badge(text)
            }
        }
        .padding(.horizontal, MonarchNotificationsBellTokens.rowHorizontalPadding)
        .padding(.vertical, MonarchNotificationsBellTokens.rowVerticalPadding)
        .frame(maxWidth: .infinity, minHeight: MonarchNotificationsBellTokens.boxSize)
    }

    private func badge(_ text: String) -> some View {
        MonarchBadge(text, tone: .danger, variant: .solid)
            .accessibilityHidden(true)
    }
}

#if DEBUG
    #Preview("MonarchNotificationsBell") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                HStack {
                    MonarchNotificationsBell {}
                    MonarchNotificationsBell(unreadCount: 3) {}
                    MonarchNotificationsBell(unreadCount: 250) {}
                    MonarchNotificationsBell(unreadCount: 3) {}.disabled(true)
                }
                MonarchNotificationsBell(variant: .row) {}
                MonarchNotificationsBell(unreadCount: 12, variant: .row) {}
            }
        }
    }
#endif
