import Foundation
import SwiftUI
import Testing

@testable import MonarchUI

private struct Message: MonarchVirtualMessageListItem {
    let id: Int
    let time: Date
    let authorID: String?
    var text: String { "Message \(id)" }
}

private struct Anonymous: MonarchVirtualMessageListItem {
    let id: Int
    let time: Date
}

private let calendar: Calendar = {
    var calendar = Calendar(identifier: .gregorian)
    calendar.timeZone = TimeZone(identifier: "UTC") ?? .gmt
    return calendar
}()
private let locale = Locale(identifier: "en_US")
/// Tuesday 10 March 2026, 12:00 UTC.
private let now = Date(timeIntervalSince1970: 1_773_144_000)
private let day: TimeInterval = 86_400

private let conversation: [Message] = [
    Message(id: 0, time: now.addingTimeInterval(-3 * day), authorID: "ana"),
    Message(id: 1, time: now.addingTimeInterval(-3 * day + 60), authorID: "ana"),
    Message(id: 2, time: now.addingTimeInterval(-day), authorID: "ben"),
    Message(id: 3, time: now.addingTimeInterval(-600), authorID: "ana"),
    Message(id: 4, time: now.addingTimeInterval(-540), authorID: "ana"),
    Message(id: 5, time: now.addingTimeInterval(-120), authorID: "ana"),
    Message(id: 6, time: now.addingTimeInterval(-60), authorID: "ben"),
]

@MainActor
private func row(_ message: Message, _ context: MonarchVirtualMessageListContext<Message>) -> some View {
    MonarchMessageRow(
        MonarchMessageRowAuthor(message.authorID == "ana" ? "Ana Lopez" : "Ben Okafor"),
        time: message.time,
        now: now,
        isGrouped: context.isGrouped,
        isHighlighted: context.isHighlighted
    ) {
        Text(message.text)
    }
}

enum MonarchVirtualMessageListFixtures: ComponentFixtures {
    static let id = "virtual-message-list"
    static let fixtures = [
        Fixture("days, unread divider, grouping and highlight") {
            MonarchVirtualMessageList(conversation, firstUnreadID: 5, highlightedID: 3, now: now, row: row)
                .frame(height: 480)
        },
        Fixture("older history, header and footer") {
            MonarchVirtualMessageList(
                conversation,
                hasOlder: true,
                now: now,
                onLoadOlder: {},
                header: { Text("The start of #design") },
                footer: { MonarchTypingIndicator(["Ben Okafor"]) },
                row: row
            )
            .frame(height: 480)
        },
        Fixture("loading and failed older pages") {
            VStack {
                MonarchVirtualMessageList(
                    conversation,
                    hasOlder: true,
                    isLoadingOlder: true,
                    now: now,
                    onLoadOlder: {},
                    row: row
                )
                MonarchVirtualMessageList(
                    conversation,
                    hasOlder: true,
                    isOlderFailed: true,
                    now: now,
                    onLoadOlder: {},
                    row: row
                )
            }
            .frame(height: 640)
        },
        Fixture("first load") {
            MonarchVirtualMessageList([Message](), isLoading: true, row: row).frame(height: 320)
        },
        Fixture("first load failed") {
            MonarchVirtualMessageList(
                [Message](),
                errorMessage: "Couldn't load this conversation.",
                onRetry: {},
                row: row
            )
            .frame(height: 320)
        },
        Fixture("empty, with the header") {
            MonarchVirtualMessageList(
                [Message](),
                header: { Text("Say hello to Ben") },
                footer: { EmptyView() },
                row: row
            )
            .frame(height: 320)
        },
        Fixture("custom labels and long text") {
            MonarchVirtualMessageList(
                conversation,
                firstUnreadID: 2,
                now: now,
                labels: MonarchVirtualMessageListLabels(newMessages: "Everything below arrived while you were away"),
                row: row
            )
            .frame(height: 480)
        },
    ]
}

@Suite("MonarchVirtualMessageList")
struct MonarchVirtualMessageListTests {
    private typealias Tokens = MonarchVirtualMessageListTokens

    private func entries(firstUnreadID: Int? = nil, window: TimeInterval = 300) -> [Tokens.Entry] {
        Tokens.entries(
            conversation,
            firstUnreadID: firstUnreadID,
            groupWindow: window,
            now: now,
            calendar: calendar,
            locale: locale,
            labels: MonarchVirtualMessageListLabels()
        )
    }

    @Test("A separator above the first message of each day")
    func days() {
        let days = entries().map(\.day)
        #expect(days == ["Saturday, March 7, 2026", nil, "Yesterday", "Today", nil, nil, nil])
    }

    @Test("Consecutive messages by one author within the window are grouped")
    func grouping() {
        #expect(entries().map(\.isGrouped) == [false, true, false, false, true, false, false])
        #expect(entries(window: 600).map(\.isGrouped) == [false, true, false, false, true, true, false])
        #expect(entries(window: 30).map(\.isGrouped) == [false, false, false, false, false, false, false])
    }

    @Test("The unread divider breaks a group")
    func unread() {
        let result = entries(firstUnreadID: 5)
        #expect(result.map(\.isFirstUnread) == [false, false, false, false, false, true, false])
        #expect(result[5].isGrouped == false)
        #expect(entries(firstUnreadID: 99).allSatisfy { !$0.isFirstUnread })
    }

    @Test("Messages without an author, or sent before the previous one, are not grouped")
    func noAuthor() {
        let anonymous = [Anonymous(id: 0, time: now), Anonymous(id: 1, time: now.addingTimeInterval(10))]
        #expect(!Tokens.isGrouped(anonymous[1], after: anonymous[0], groupWindow: 300))
        let early = Message(id: 9, time: now.addingTimeInterval(-10), authorID: "ana")
        let late = Message(id: 8, time: now, authorID: "ana")
        #expect(!Tokens.isGrouped(early, after: late, groupWindow: 300))
        #expect(!Tokens.isGrouped(late, after: nil, groupWindow: 300))
    }

    @Test("Day labels follow the locale and calendar")
    func dayLabel() {
        let labels = MonarchVirtualMessageListLabels()
        #expect(Tokens.dayLabel(now, now: now, calendar: calendar, locale: locale, labels: labels) == "Today")
        let french = Tokens.dayLabel(
            now.addingTimeInterval(-3 * day),
            now: now,
            calendar: calendar,
            locale: Locale(identifier: "fr_FR"),
            labels: labels
        )
        #expect(french == "samedi 7 mars 2026")
        let custom = MonarchVirtualMessageListLabels(today: "Aujourd’hui", yesterday: "Hier")
        #expect(Tokens.dayLabel(now, now: now, calendar: calendar, locale: locale, labels: custom) == "Aujourd’hui")
        let yesterday = now.addingTimeInterval(-day)
        #expect(Tokens.dayLabel(yesterday, now: now, calendar: calendar, locale: locale, labels: custom) == "Hier")
    }

    @Test("Only messages added at the end count as arrivals")
    func arrivals() {
        #expect(Tokens.arrivedCount(oldLastID: 3, newIDs: [1, 2, 3, 4, 5]) == 2)
        #expect(Tokens.arrivedCount(oldLastID: 3, newIDs: [0, 1, 2, 3]) == 0)
        #expect(Tokens.arrivedCount(oldLastID: nil as Int?, newIDs: [1, 2]) == 0)
        #expect(Tokens.arrivedCount(oldLastID: 3, newIDs: [7, 8]) == 0)
        #expect(Tokens.arrivedCount(oldLastID: 3, newIDs: [Int]()) == 0)
    }

    @Test("The jump button counts unseen messages and shows only when scrolled up")
    func jump() {
        let labels = MonarchVirtualMessageListLabels()
        #expect(Tokens.jumpTitle(unseen: 0, labels: labels) == "Jump to latest")
        #expect(Tokens.jumpTitle(unseen: 1, labels: labels) == "1 new message")
        #expect(Tokens.jumpTitle(unseen: 3, labels: labels) == "3 new messages")
        #expect(Tokens.arrivedText(2, labels: labels) == "2 new messages")
        #expect(Tokens.jumpTitle(unseen: 2, labels: MonarchVirtualMessageListLabels(unseen: { "+\($0)" })) == "+2")
        #expect(Tokens.showsJump(isAtBottom: false, showsJumpToLatest: true, count: 3))
        #expect(!Tokens.showsJump(isAtBottom: true, showsJumpToLatest: true, count: 3))
        #expect(!Tokens.showsJump(isAtBottom: false, showsJumpToLatest: false, count: 3))
        #expect(!Tokens.showsJump(isAtBottom: false, showsJumpToLatest: true, count: 0))
        #expect(Tokens.follows(isAtBottom: true, stickToBottom: true))
        #expect(!Tokens.follows(isAtBottom: false, stickToBottom: true))
        #expect(!Tokens.follows(isAtBottom: true, stickToBottom: false))
    }

    @Test("Loading, failure, empty and messages")
    func content() {
        #expect(Tokens.content(count: 0, isLoading: true, isFailed: false) == .loading)
        #expect(Tokens.content(count: 0, isLoading: true, isFailed: true) == .failed)
        #expect(Tokens.content(count: 0, isLoading: false, isFailed: false) == .empty)
        #expect(Tokens.content(count: 2, isLoading: true, isFailed: true) == .messages)
    }

    @Test("The load-older row needs the action and something to show")
    func loadOlder() {
        #expect(Tokens.showsLoadOlder(hasOlder: true, isLoadingOlder: false, isOlderFailed: false, hasAction: true))
        #expect(Tokens.showsLoadOlder(hasOlder: false, isLoadingOlder: false, isOlderFailed: true, hasAction: true))
        #expect(!Tokens.showsLoadOlder(hasOlder: true, isLoadingOlder: false, isOlderFailed: false, hasAction: false))
        #expect(!Tokens.showsLoadOlder(hasOlder: false, isLoadingOlder: false, isOlderFailed: false, hasAction: true))
    }

    @Test("Built-in strings resolve")
    func strings() {
        let strings = [
            MonarchVirtualMessageListStrings.list, MonarchVirtualMessageListStrings.newMessages,
            MonarchVirtualMessageListStrings.today, MonarchVirtualMessageListStrings.yesterday,
            MonarchVirtualMessageListStrings.jumpToLatest, MonarchVirtualMessageListStrings.loadOlder,
            MonarchVirtualMessageListStrings.loadingOlder, MonarchVirtualMessageListStrings.olderFailed,
            MonarchVirtualMessageListStrings.loading, MonarchVirtualMessageListStrings.newCount(2),
        ]
        for string in strings {
            #expect(!string.hasPrefix("monarch."))
            #expect(!string.isEmpty)
        }
    }
}
