import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and logic for ``MonarchNotificationCenter``.
enum MonarchNotificationCenterTokens {
    /// The number of placeholder rows while loading.
    static let placeholderRows = 3
    /// Space above and below a placeholder row.
    static let placeholderSpacing: CGFloat = MonarchTokens.Space.space2
    /// The shape of a placeholder row's text. Always redacted, never shown or read.
    static let placeholderTitle = "Placeholder notification title"
    static let placeholderMessage = "Placeholder notification message"
    /// The highest unread count shown in full; above it the tab shows "99+".
    static let maxShownCount = 99
    /// The SF Symbol of the empty state.
    static let emptySymbol = "bell"
    /// The SF Symbol of a notification with no actor and no symbol of its own.
    static let defaultSymbol = "bell"
    /// The SF Symbol of the mark-as-read swipe action.
    static let markReadSymbol = "checkmark"
    /// The tint of the mark-as-read swipe action.
    static var markReadTint: MonarchColorRole { \.infoSolid }
    /// The tone of the unread count on the Unread tab: accent, as on the web.
    static let countTone: MonarchTone = .accent

    /// A day of notifications.
    struct DayGroup: Identifiable, Equatable {
        /// The start of the day.
        let day: Date
        /// The day's items, in the order given.
        let items: [MonarchNotificationCenterItem]
        /// The start of the day, which is unique.
        var id: Date { day }
    }

    /// The unread total: the server's count, or the unread loaded items.
    static func unreadCount(_ items: [MonarchNotificationCenterItem], override: Int?) -> Int {
        override ?? items.filter { !$0.isRead }.count
    }

    /// The items shown for a tab.
    static func visible(
        _ items: [MonarchNotificationCenterItem],
        filter: MonarchNotificationCenterFilter
    ) -> [MonarchNotificationCenterItem] {
        filter == .unread ? items.filter { !$0.isRead } : items
    }

    /// The tab's unread count, "99+" above 99, or `nil` for zero.
    static func badgeText(_ count: Int, locale: Locale) -> String? {
        guard count > 0 else { return nil }
        if count > maxShownCount { return "\(maxShownCount.formatted(.number.locale(locale)))+" }
        return count.formatted(.number.locale(locale))
    }

    /// Groups items by the day they happened, keeping their order.
    static func groups(_ items: [MonarchNotificationCenterItem], calendar: Calendar) -> [DayGroup] {
        var order: [Date] = []
        var byDay: [Date: [MonarchNotificationCenterItem]] = [:]
        for item in items {
            let day = calendar.startOfDay(for: item.time)
            if byDay[day] == nil { order.append(day) }
            byDay[day, default: []].append(item)
        }
        return order.map { DayGroup(day: $0, items: byDay[$0] ?? []) }
    }

    /// The heading of a day: "Today", "Yesterday", then "Monday, 2 March",
    /// with the year when it is not the current year.
    static func dayLabel(
        _ day: Date,
        now: Date,
        calendar: Calendar,
        locale: Locale,
        labels: MonarchNotificationCenterLabels
    ) -> String {
        if calendar.isDate(day, inSameDayAs: now) {
            return labels.today ?? MonarchNotificationCenterStrings.today
        }
        if let yesterday = calendar.date(byAdding: .day, value: -1, to: now),
            calendar.isDate(day, inSameDayAs: yesterday)
        {
            return labels.yesterday ?? MonarchNotificationCenterStrings.yesterday
        }
        var style = Date.FormatStyle(locale: locale, calendar: calendar, timeZone: calendar.timeZone)
            .weekday(.wide).day().month(.wide)
        if calendar.component(.year, from: day) != calendar.component(.year, from: now) {
            style = style.year()
        }
        return day.formatted(style)
    }

    /// The empty state's title for a tab.
    static func emptyTitle(_ filter: MonarchNotificationCenterFilter, labels: MonarchNotificationCenterLabels) -> String
    {
        filter == .unread
            ? labels.emptyUnreadTitle ?? MonarchNotificationCenterStrings.emptyUnreadTitle
            : labels.emptyTitle ?? MonarchNotificationCenterStrings.emptyTitle
    }

    /// The empty state's kind: a success in the Unread tab, a first run otherwise.
    static func emptyKind(_ filter: MonarchNotificationCenterFilter) -> MonarchEmptyStateKind {
        filter == .unread ? .success : .firstRun
    }

    /// Whether activating an item marks it read.
    static func marksRead(_ item: MonarchNotificationCenterItem, marksReadOnSelect: Bool) -> Bool {
        !item.isRead && marksReadOnSelect
    }

    /// What the list shows.
    enum Content: Equatable {
        case loading
        case failed(String)
        case empty
        case grouped
        case flat
    }

    /// What the list shows: loading wins, then an error, then empty, then the items.
    static func content(
        isLoading: Bool,
        errorMessage: String?,
        visibleCount: Int,
        groupsByDay: Bool
    ) -> Content {
        if isLoading { return .loading }
        if let errorMessage, !errorMessage.isEmpty { return .failed(errorMessage) }
        if visibleCount == 0 { return .empty }
        return groupsByDay ? .grouped : .flat
    }

    /// Whether the load-more row is shown.
    static func showsLoadMore(hasMore: Bool, canLoad: Bool, content: Content) -> Bool {
        hasMore && canLoad && (content == .grouped || content == .flat)
    }
}
