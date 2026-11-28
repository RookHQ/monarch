import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and rules for ``MonarchAiThread``.
enum MonarchAiThreadTokens {
    /// The space between two messages.
    static let rowSpacing: CGFloat = MonarchTokens.Space.space4
    /// Padding around the messages inside the scrolling area.
    static let contentPadding: CGFloat = MonarchTokens.Space.space4
    /// The space between the jump button and the edges of the list.
    static let jumpPadding: CGFloat = MonarchTokens.Space.space3
    /// Padding around the notice under the list.
    static let noticePadding: CGFloat = MonarchTokens.Space.space2
    /// The symbol of the jump button.
    static let jumpSymbol = "arrow.down"
    /// The id of the invisible view under the last row.
    static let bottomID = "monarch.aiThread.bottom"

    /// A notice under the list.
    enum Notice: Equatable, Sendable {
        /// The device is offline; the count of queued messages.
        case offline(queued: Int)
        /// Back online, sending queued messages.
        case sending(queued: Int)
        /// A running reply is slow.
        case slow

        /// The tone of the notice: warning for the connection, neutral for a slow reply.
        var tone: MonarchTone {
            switch self {
            case .offline, .sending: .warning
            case .slow: .neutral
            }
        }

        /// The SF Symbol of the notice.
        var systemImage: String {
            switch self {
            case .offline: "wifi.slash"
            case .sending: "arrow.up.circle"
            case .slow: "clock"
            }
        }
    }

    /// Whether a reply is running: submitted or streaming.
    static func isRunning(_ status: MonarchAiConversationStatus) -> Bool {
        status == .submitted || status == .streaming
    }

    /// One notice at a time, the most important first. Offline wins: a reply
    /// that is slow because the connection is gone is not "still working".
    static func notice(
        isOffline: Bool,
        queuedCount: Int,
        isSlow: Bool,
        status: MonarchAiConversationStatus
    ) -> Notice? {
        if isOffline { return .offline(queued: max(0, queuedCount)) }
        if queuedCount > 0 { return .sending(queued: queuedCount) }
        if isSlow && isRunning(status) { return .slow }
        return nil
    }

    /// The text of a notice.
    static func noticeText(_ notice: Notice, labels: MonarchAiThreadLabels) -> String {
        switch notice {
        case .offline(let queued) where queued > 0:
            labels.offlineQueued?(queued) ?? MonarchAiThreadStrings.offlineQueued(queued)
        case .offline:
            labels.offline ?? MonarchAiThreadStrings.offline
        case .sending(let queued):
            labels.sendingQueued?(queued) ?? MonarchAiThreadStrings.sendingQueued(queued)
        case .slow:
            labels.slow ?? MonarchAiThreadStrings.slow
        }
    }

    /// Whether the empty content replaces the list: no messages, nothing to
    /// load and no submitted reply.
    static func showsEmpty(
        count: Int,
        hasEarlier: Bool,
        canLoadEarlier: Bool,
        status: MonarchAiConversationStatus
    ) -> Bool {
        count == 0 && !(hasEarlier && canLoadEarlier) && status != .submitted
    }

    /// Whether "Load earlier messages" is the top row.
    static func showsLoadEarlier(hasEarlier: Bool, isLoadingEarlier: Bool, canLoadEarlier: Bool) -> Bool {
        canLoadEarlier && (hasEarlier || isLoadingEarlier)
    }

    /// For each message, the date of its day label when it is the first
    /// message of a calendar day, or `nil`. A message whose `createdAt` is
    /// not an ISO 8601 time gets no label, and the next one starts a new day.
    static func dayStarts(_ messages: [MonarchAiConversationMessage], calendar: Calendar) -> [Date?] {
        var result: [Date?] = []
        result.reserveCapacity(messages.count)
        var previous: Date?
        for message in messages {
            guard let date = message.createdDate else {
                result.append(nil)
                previous = nil
                continue
            }
            let isNewDay = previous.map { !calendar.isDate($0, inSameDayAs: date) } ?? true
            result.append(isNewDay ? date : nil)
            previous = date
        }
        return result
    }

    /// The day label override for ``MonarchAiContextDivider``: the app's
    /// "Today" or "Yesterday", or `nil` to keep the divider's own label.
    static func dayLabel(_ date: Date, now: Date, calendar: Calendar, labels: MonarchAiThreadLabels) -> String? {
        if calendar.isDate(date, inSameDayAs: now) { return labels.today }
        if let yesterday = calendar.date(byAdding: .day, value: -1, to: now),
            calendar.isDate(date, inSameDayAs: yesterday)
        {
            return labels.yesterday
        }
        return nil
    }

    /// How many messages came after the last one the person saw at the bottom.
    static func newCount(ids: [String], lastSeenID: String?) -> Int {
        guard let lastSeenID, let index = ids.lastIndex(of: lastSeenID) else { return 0 }
        return ids.count - 1 - index
    }

    /// The jump button's title: the count of new messages, or "Jump to latest".
    static func jumpTitle(newCount: Int, labels: MonarchAiThreadLabels) -> String {
        guard newCount > 0 else { return labels.jumpToLatest ?? MonarchAiThreadStrings.jumpToLatest }
        return labels.newMessages?(newCount) ?? MonarchAiThreadStrings.newMessages(newCount)
    }

    /// The jump button's spoken name: "Jump to latest, 2 new messages".
    static func jumpAccessibilityLabel(newCount: Int, labels: MonarchAiThreadLabels) -> String {
        let jump = labels.jumpToLatest ?? MonarchAiThreadStrings.jumpToLatest
        guard newCount > 0 else { return jump }
        return "\(jump), \(labels.newMessages?(newCount) ?? MonarchAiThreadStrings.newMessages(newCount))"
    }

    /// Whether the jump button shows: away from the newest row, with messages.
    static func showsJump(isAtBottom: Bool, count: Int) -> Bool {
        !isAtBottom && count > 0
    }

    /// The assertive announcement of a conversation error that the newest
    /// message does not show as an error part of its own.
    static func errorAnnouncement(
        _ error: MonarchAiConversationError?,
        last: MonarchAiConversationMessage?,
        labels: MonarchAiThreadLabels
    ) -> String? {
        guard let error else { return nil }
        let isShown =
            last?.parts.contains { part in
                if case .error = part { return true }
                return false
            } ?? false
        guard !isShown else { return nil }
        return labels.error?(error.message) ?? MonarchAiThreadStrings.error(error.message)
    }

    /// The fixed words of the announcer.
    static func words(_ labels: MonarchAiThreadLabels) -> MonarchAiThreadAnnouncer.Words {
        MonarchAiThreadAnnouncer.Words(
            thinking: labels.thinking ?? MonarchAiThreadStrings.thinking,
            complete: labels.responseComplete ?? MonarchAiThreadStrings.responseComplete,
            stopped: labels.responseStopped ?? MonarchAiThreadStrings.responseStopped
        )
    }
}
