import Foundation
import SwiftUI
import Testing

@testable import MonarchUI

private let now = Date(timeIntervalSince1970: 1_772_452_800)  // 2026-03-02 12:00 UTC
private let items = [
    MonarchNotificationCenterItem(
        id: "n1",
        title: "Ana Lopez mentioned you in Launch checklist",
        message: "Can you confirm the legal sign-off date?",
        time: now.addingTimeInterval(-1_080),
        actorName: "Ana Lopez"
    ),
    MonarchNotificationCenterItem(
        id: "n2",
        title: "Ben Okafor assigned you Update pricing page",
        time: now.addingTimeInterval(-10_500),
        actorName: "Ben Okafor"
    ),
    MonarchNotificationCenterItem(
        id: "n3",
        title: "Chloé Martin replied to your comment",
        message: "Agreed, let's ship the shorter version.",
        time: now.addingTimeInterval(-86_400),
        isRead: true,
        actorName: "Chloé Martin"
    ),
    MonarchNotificationCenterItem(
        id: "n4",
        title: "Your weekly export is ready",
        time: now.addingTimeInterval(-86_400 * 3),
        isRead: true,
        systemImage: "arrow.down.doc",
        systemImageLabel: "Export"
    ),
]

enum MonarchNotificationCenterFixtures: ComponentFixtures {
    static let id = "notification-center"
    static let fixtures = [
        Fixture("grouped by day with load more and footer") {
            NavigationStack {
                MonarchNotificationCenter(
                    items,
                    now: now,
                    hasMore: true,
                    onMarkRead: { _ in },
                    onMarkAllRead: {},
                    onLoadMore: {}
                ) {
                    Button("Notification settings") {}.buttonStyle(.monarchLink(.standalone))
                }
            }
            .frame(height: 640)
        },
        Fixture("unread tab, flat, count above 99") {
            NavigationStack {
                MonarchNotificationCenter(
                    items,
                    unreadCount: 250,
                    filter: .unread,
                    now: now,
                    groupsByDay: false,
                    showsDoneButton: false
                )
            }
            .frame(height: 480)
        },
        Fixture("loading and load-more states") {
            VStack {
                NavigationStack { MonarchNotificationCenter([], now: now, isLoading: true) }
                NavigationStack {
                    MonarchNotificationCenter(items, now: now, hasMore: true, isLoadingMore: true, onLoadMore: {})
                }
                NavigationStack {
                    MonarchNotificationCenter(items, now: now, hasMore: true, isLoadMoreFailed: true, onLoadMore: {})
                }
            }
            .frame(height: 900)
        },
        Fixture("error") {
            NavigationStack {
                MonarchNotificationCenter([], now: now, errorMessage: "Couldn't load notifications", onRetry: {})
            }
            .frame(height: 420)
        },
        Fixture("empty, and empty unread") {
            VStack {
                NavigationStack { MonarchNotificationCenter([], now: now) }
                NavigationStack {
                    MonarchNotificationCenter(Array(items.suffix(2)), filter: .unread, now: now, onMarkAllRead: {})
                }
            }
            .frame(height: 800)
        },
    ]
}

@Suite("MonarchNotificationCenter")
@MainActor
struct MonarchNotificationCenterTests {
    typealias Tokens = MonarchNotificationCenterTokens
    let labels = MonarchNotificationCenterLabels()
    let english = Locale(identifier: "en_GB")
    var utc: Calendar {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(identifier: "UTC") ?? .gmt
        calendar.locale = Locale(identifier: "en_GB")
        return calendar
    }

    @Test("The unread total is the server's count or the unread loaded items")
    func unreadCount() {
        #expect(Tokens.unreadCount(items, override: nil) == 2)
        #expect(Tokens.unreadCount(items, override: 40) == 40)
        #expect(Tokens.unreadCount([], override: nil) == 0)
    }

    @Test("The Unread tab shows only unread items, in order")
    func visible() {
        #expect(Tokens.visible(items, filter: .all).map(\.id) == ["n1", "n2", "n3", "n4"])
        #expect(Tokens.visible(items, filter: .unread).map(\.id) == ["n1", "n2"])
    }

    @Test("The tab count shows 99+ above 99 and nothing for zero")
    func badgeText() {
        #expect(Tokens.badgeText(0, locale: english) == nil)
        #expect(Tokens.badgeText(7, locale: english) == "7")
        #expect(Tokens.badgeText(99, locale: english) == "99")
        #expect(Tokens.badgeText(250, locale: english) == "99+")
    }

    @Test("Items are grouped by day in the order given")
    func groups() {
        let groups = Tokens.groups(items, calendar: utc)
        #expect(groups.count == 3)
        #expect(groups[0].items.map(\.id) == ["n1", "n2"])
        #expect(groups[1].items.map(\.id) == ["n3"])
        #expect(groups[2].items.map(\.id) == ["n4"])
        #expect(Tokens.groups([], calendar: utc).isEmpty)
    }

    @Test("Day headings are Today, Yesterday, then the weekday and date")
    func dayLabel() {
        func label(_ days: Double) -> String {
            Tokens.dayLabel(
                utc.startOfDay(for: now.addingTimeInterval(-86_400 * days)),
                now: now,
                calendar: utc,
                locale: english,
                labels: labels
            )
        }
        #expect(label(0) == "Today")
        #expect(label(1) == "Yesterday")
        #expect(label(3) == "Friday 27 February")
        #expect(label(400).contains("2025"))
    }

    @Test("The list shows loading, then the error, then empty, then items")
    func content() {
        #expect(Tokens.content(isLoading: true, errorMessage: "x", visibleCount: 3, groupsByDay: true) == .loading)
        #expect(Tokens.content(isLoading: false, errorMessage: "x", visibleCount: 3, groupsByDay: true) == .failed("x"))
        #expect(Tokens.content(isLoading: false, errorMessage: "", visibleCount: 0, groupsByDay: true) == .empty)
        #expect(Tokens.content(isLoading: false, errorMessage: nil, visibleCount: 2, groupsByDay: true) == .grouped)
        #expect(Tokens.content(isLoading: false, errorMessage: nil, visibleCount: 2, groupsByDay: false) == .flat)
    }

    @Test("Load more shows only with more pages, a handler and a list")
    func showsLoadMore() {
        #expect(Tokens.showsLoadMore(hasMore: true, canLoad: true, content: .grouped))
        #expect(Tokens.showsLoadMore(hasMore: true, canLoad: true, content: .flat))
        #expect(!Tokens.showsLoadMore(hasMore: false, canLoad: true, content: .grouped))
        #expect(!Tokens.showsLoadMore(hasMore: true, canLoad: false, content: .grouped))
        #expect(!Tokens.showsLoadMore(hasMore: true, canLoad: true, content: .loading))
        #expect(!Tokens.showsLoadMore(hasMore: true, canLoad: true, content: .empty))
    }

    @Test("Activating an unread item marks it read unless turned off")
    func marksRead() {
        #expect(Tokens.marksRead(items[0], marksReadOnSelect: true))
        #expect(!Tokens.marksRead(items[0], marksReadOnSelect: false))
        #expect(!Tokens.marksRead(items[2], marksReadOnSelect: true))
    }

    @Test("The empty state depends on the tab")
    func empty() {
        #expect(Tokens.emptyTitle(.all, labels: labels) == "You’re all caught up")
        #expect(Tokens.emptyTitle(.unread, labels: labels) == "No unread notifications")
        #expect(Tokens.emptyKind(.all) == .firstRun)
        #expect(Tokens.emptyKind(.unread) == .success)
    }

    @Test("Overrides replace the built-in strings")
    func overrides() {
        let custom = MonarchNotificationCenterLabels(today: "Heute", emptyTitle: "Alles erledigt")
        #expect(Tokens.dayLabel(now, now: now, calendar: utc, locale: english, labels: custom) == "Heute")
        #expect(Tokens.emptyTitle(.all, labels: custom) == "Alles erledigt")
    }

    @Test("The built-in strings come from the component's strings table")
    func strings() {
        #expect(MonarchNotificationCenterStrings.title == "Notifications")
        #expect(MonarchNotificationCenterStrings.all == "All")
        #expect(MonarchNotificationCenterStrings.unread == "Unread")
        #expect(MonarchNotificationCenterStrings.filter == "Show")
        #expect(MonarchNotificationCenterStrings.markAllRead == "Mark all as read")
        #expect(MonarchNotificationCenterStrings.markRead == "Mark as read")
        #expect(MonarchNotificationCenterStrings.unreadItem == "Unread")
        #expect(MonarchNotificationCenterStrings.emptyMessage == "New activity will appear here.")
        #expect(MonarchNotificationCenterStrings.loading == "Loading notifications")
        #expect(MonarchNotificationCenterStrings.done == "Done")
    }
}
