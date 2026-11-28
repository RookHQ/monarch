import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and rules for ``MonarchVirtualMessageList``.
enum MonarchVirtualMessageListTokens {
    /// Messages by one author closer together than this are grouped: five minutes.
    static let defaultGroupWindow: TimeInterval = 300
    /// The gap around a divider's text.
    static let dividerSpacing: CGFloat = MonarchTokens.Space.space2
    /// Padding above and below a divider.
    static let dividerPadding: CGFloat = MonarchTokens.Space.space2
    /// Padding beside dividers, matching the message rows.
    static let horizontalPadding: CGFloat = MonarchTokens.Space.space4
    /// The space between the jump button and the edges of the list.
    static let jumpPadding: CGFloat = MonarchTokens.Space.space3
    /// The symbol of the jump button.
    static let jumpSymbol = "arrow.down"
    /// The number of placeholder rows while the first messages load.
    static let placeholderRows = 4
    /// The id of the invisible view under the last message.
    static let bottomID = "monarch.virtualMessageList.bottom"
    /// The role of a day separator's text.
    static var dayText: MonarchColorRole { \.textMuted }
    /// The role of the unread divider's text: the accent tone, as on the web.
    static var unreadText: MonarchColorRole { MonarchTonePalette.palette(for: .accent).strong }
    /// The role of the unread divider's line.
    static var unreadLine: MonarchColorRole { MonarchTonePalette.palette(for: .accent).border }

    /// What the list shows.
    enum Content: Equatable {
        /// Placeholder rows: the first load.
        case loading
        /// The first load failed.
        case failed
        /// No messages: the header.
        case empty
        /// The messages.
        case messages
    }

    /// What is drawn above one message.
    struct Entry: Equatable {
        /// The day separator's text, above the first message of a day.
        let day: String?
        /// Whether the unread divider is above this message.
        let isFirstUnread: Bool
        /// Whether the message continues the previous one's group.
        let isGrouped: Bool
    }

    /// The content: a failure wins, then loading, while there are no messages.
    static func content(count: Int, isLoading: Bool, isFailed: Bool) -> Content {
        guard count == 0 else { return .messages }
        if isFailed { return .failed }
        return isLoading ? .loading : .empty
    }

    /// The day separators, unread divider and grouping of every message.
    static func entries<Item: MonarchVirtualMessageListItem>(
        _ items: [Item],
        firstUnreadID: Item.ID?,
        groupWindow: TimeInterval,
        now: Date,
        calendar: Calendar,
        locale: Locale,
        labels: MonarchVirtualMessageListLabels
    ) -> [Entry] {
        var result: [Entry] = []
        result.reserveCapacity(items.count)
        var previous: Item?
        for item in items {
            let isNewDay = previous.map { !calendar.isDate($0.time, inSameDayAs: item.time) } ?? true
            let isFirstUnread = firstUnreadID.map { $0 == item.id } ?? false
            let day = isNewDay ? dayLabel(item.time, now: now, calendar: calendar, locale: locale, labels: labels) : nil
            let grouped =
                !isNewDay && !isFirstUnread
                && isGrouped(item, after: previous, groupWindow: groupWindow)
            result.append(Entry(day: day, isFirstUnread: isFirstUnread, isGrouped: grouped))
            previous = item
        }
        return result
    }

    /// Whether a message continues the previous one's group: the same known
    /// author, sent no earlier and within the window.
    static func isGrouped<Item: MonarchVirtualMessageListItem>(
        _ item: Item,
        after previous: Item?,
        groupWindow: TimeInterval
    ) -> Bool {
        guard let previous, let author = item.authorID, author == previous.authorID else { return false }
        let gap = item.time.timeIntervalSince(previous.time)
        return gap >= 0 && gap <= groupWindow
    }

    /// "Today", "Yesterday", or the full date in the locale and calendar.
    static func dayLabel(
        _ date: Date,
        now: Date,
        calendar: Calendar,
        locale: Locale,
        labels: MonarchVirtualMessageListLabels
    ) -> String {
        if calendar.isDate(date, inSameDayAs: now) {
            return labels.today ?? MonarchVirtualMessageListStrings.today
        }
        if let yesterday = calendar.date(byAdding: .day, value: -1, to: now),
            calendar.isDate(date, inSameDayAs: yesterday)
        {
            return labels.yesterday ?? MonarchVirtualMessageListStrings.yesterday
        }
        let style = Date.FormatStyle(
            date: .complete,
            time: .omitted,
            locale: locale,
            calendar: calendar,
            timeZone: calendar.timeZone
        )
        return date.formatted(style)
    }

    /// How many messages arrived at the end between two lists of ids; 0 on
    /// the first load, when older messages were added above, or when the
    /// list was replaced.
    static func arrivedCount<ID: Hashable>(oldLastID: ID?, newIDs: [ID]) -> Int {
        guard let oldLastID, let newLast = newIDs.last, newLast != oldLastID,
            let index = newIDs.lastIndex(of: oldLastID)
        else { return 0 }
        return newIDs.count - 1 - index
    }

    /// The jump button's title: the unseen count, or "Jump to latest".
    static func jumpTitle(unseen: Int, labels: MonarchVirtualMessageListLabels) -> String {
        guard unseen > 0 else { return labels.jumpToLatest ?? MonarchVirtualMessageListStrings.jumpToLatest }
        return labels.unseen?(unseen) ?? MonarchVirtualMessageListStrings.newCount(unseen)
    }

    /// Announced when messages arrive.
    static func arrivedText(_ count: Int, labels: MonarchVirtualMessageListLabels) -> String {
        labels.arrived?(count) ?? MonarchVirtualMessageListStrings.newCount(count)
    }

    /// Whether the jump button shows: away from the newest message, with messages, when wanted.
    static func showsJump(isAtBottom: Bool, showsJumpToLatest: Bool, count: Int) -> Bool {
        showsJumpToLatest && !isAtBottom && count > 0
    }

    /// Whether arriving messages scroll into view: the reader is at the newest message and following is on.
    static func follows(isAtBottom: Bool, stickToBottom: Bool) -> Bool {
        isAtBottom && stickToBottom
    }

    /// Whether the load-older row is shown above the messages.
    static func showsLoadOlder(hasOlder: Bool, isLoadingOlder: Bool, isOlderFailed: Bool, hasAction: Bool) -> Bool {
        hasAction && (hasOlder || isLoadingOlder || isOlderFailed)
    }
}
