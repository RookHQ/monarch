import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchInboxRow``.
enum MonarchInboxRowTokens {
    /// The width of the accent bar on the open conversation.
    static let currentBarWidth: CGFloat = 3
    /// How far the open conversation's background reaches past the row's padding, to the list edges.
    static let currentBleed: CGFloat = 16
    /// The gap between the avatar and the text.
    static let leadingSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the lines of the row.
    static let lineSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The gap between items on a line.
    static let itemSpacing: CGFloat = MonarchTokens.Space.space1_5
    /// The size of the avatar.
    static let avatarSize: MonarchAvatarSize = .lg
    /// The diameter of the unread dot.
    static let dotDiameter: CGFloat = MonarchTokens.Space.space2
    /// Padding before and after the unread count.
    static let countPadding: CGFloat = MonarchTokens.Space.space1_5
    /// The least width of the unread count, so one digit makes a circle.
    static let countMinWidth: CGFloat = MonarchTokens.Space.space5
    /// The highest count shown in full; above it the row shows "99+".
    static let maxShownCount = 99
    /// The SF Symbols of the swipe actions.
    static let markReadSymbol = "envelope.open"
    static let markUnreadSymbol = "envelope.badge"
    static let archiveSymbol = "archivebox"
    static let deleteSymbol = "trash"

    /// The colour roles of the unread mark and count.
    static var unreadFill: MonarchColorRole { \.accentSolid }
    static var unreadForeground: MonarchColorRole { \.accentOnSolid }

    /// The tint of a swipe action, by kind.
    static func tint(_ action: MonarchInboxRowSwipeAction) -> MonarchColorRole {
        switch action {
        case .toggleRead: \.infoSolid
        case .archive: \.accentSolid
        case .delete: \.dangerSolid
        }
    }

    /// Whether a swipe action goes on the leading edge, as in Mail.
    static func isLeading(_ action: MonarchInboxRowSwipeAction) -> Bool {
        if case .toggleRead = action { return true }
        return false
    }

    /// The title of a swipe action. Toggle read is named after what it does
    /// to the row as it is now.
    static func title(_ action: MonarchInboxRowSwipeAction, isUnread: Bool, labels: MonarchInboxRowLabels) -> String {
        switch action {
        case .toggleRead:
            isUnread
                ? labels.markRead ?? MonarchInboxRowStrings.markRead
                : labels.markUnread ?? MonarchInboxRowStrings.markUnread
        case .archive: labels.archive ?? MonarchInboxRowStrings.archive
        case .delete: labels.delete ?? MonarchInboxRowStrings.delete
        }
    }

    /// The SF Symbol of a swipe action.
    static func symbol(_ action: MonarchInboxRowSwipeAction, isUnread: Bool) -> String {
        switch action {
        case .toggleRead: isUnread ? markReadSymbol : markUnreadSymbol
        case .archive: archiveSymbol
        case .delete: deleteSymbol
        }
    }

    /// Whether the row is unread: the flag, or a count above zero.
    static func isUnread(_ isUnread: Bool, count: Int?) -> Bool {
        isUnread || (count ?? 0) > 0
    }

    /// The visible unread count: the number up to 99, then "99+". `nil`
    /// when there is no count to show.
    static func countText(_ count: Int?) -> String? {
        guard let count, count > 0 else { return nil }
        return count > maxShownCount ? "\(maxShownCount.formatted())+" : count.formatted()
    }

    /// The spoken unread state, or `nil` for a read row.
    static func unreadLabel(isUnread: Bool, count: Int?, labels: MonarchInboxRowLabels) -> String? {
        guard Self.isUnread(isUnread, count: count) else { return nil }
        let value = (count ?? 0) > 0 ? count : nil
        return labels.unread?(value) ?? MonarchInboxRowStrings.unread(value)
    }

    /// What VoiceOver reads for the title: the title, then the unread state.
    static func titleLabel(_ title: String, unread: String?) -> String {
        guard let unread else { return title }
        return MonarchInboxRowStrings.titleWithState(title, unread)
    }

    /// The compact age: "now", "5m", "3h", "2d", "4w", then a short date,
    /// "12 Mar", with the year when it is not the current year.
    static func compactAge(
        _ date: Date,
        now: Date,
        labels: MonarchInboxRowLabels,
        locale: Locale,
        calendar: Calendar
    ) -> String {
        let minutes = Int(max(0, now.timeIntervalSince(date)) / 60)
        func unit(_ unit: MonarchInboxRowAgeUnit, _ value: Int) -> String {
            labels.age?(unit, value) ?? MonarchInboxRowStrings.age(unit, value)
        }
        if minutes < 1 { return unit(.now, 0) }
        if minutes < 60 { return unit(.minutes, minutes) }
        let hours = minutes / 60
        if hours < 24 { return unit(.hours, hours) }
        let days = hours / 24
        if days < 7 { return unit(.days, days) }
        if days < 30 { return unit(.weeks, days / 7) }
        var style = Date.FormatStyle(locale: locale, calendar: calendar, timeZone: calendar.timeZone).day().month(
            .abbreviated
        )
        if calendar.component(.year, from: date) != calendar.component(.year, from: now) {
            style = style.year()
        }
        return date.formatted(style)
    }

    /// The time written in `style`: the compact age, or the full relative time
    /// ("5 minutes ago", "yesterday") by the shared rule.
    static func timeText(
        _ date: Date,
        style: MonarchInboxRowTimeStyle,
        now: Date,
        labels: MonarchInboxRowLabels,
        locale: Locale,
        calendar: Calendar
    ) -> String {
        switch style {
        case .compact: compactAge(date, now: now, labels: labels, locale: locale, calendar: calendar)
        case .relative: MonarchRelativeTime.string(date, now: now, locale: locale, calendar: calendar)
        }
    }

    /// The full date and time read by VoiceOver.
    static func absoluteTime(_ date: Date, locale: Locale, calendar: Calendar) -> String {
        date.formatted(
            Date.FormatStyle(
                date: .abbreviated,
                time: .omitted,
                locale: locale,
                calendar: calendar,
                timeZone: calendar.timeZone
            ).monarchTime()
        )
    }
}
