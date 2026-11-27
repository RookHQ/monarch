import Foundation
import SwiftUI
import Testing

@testable import MonarchUI

private let now = Date(timeIntervalSinceReferenceDate: 800_000_000)

enum MonarchInboxRowFixtures: ComponentFixtures {
    static let id = "inbox-row"
    static let fixtures = [
        Fixture("unread count, channel, badges and trailing") {
            List {
                MonarchInboxRow(
                    "Ana Silva",
                    preview: "Can you look at the invoice before Friday?",
                    timestamp: now.addingTimeInterval(-300),
                    now: now,
                    systemImage: "envelope",
                    systemImageLabel: "via email",
                    unreadCount: 3,
                    swipeActions: [.toggleRead {}, .archive {}, .delete {}]
                ) {
                    MonarchBadge("Open", tone: .info)
                } trailing: {
                    Text("#1042").monarchTextStyle(.caption)
                }
            }
        },
        Fixture("unread dot, read, no avatar") {
            VStack(alignment: .leading) {
                MonarchInboxRow("Ben Okafor", preview: "Thanks!", timestamp: now, now: now, isUnread: true)
                MonarchInboxRow(
                    "Design team",
                    preview: "New file shared",
                    timestamp: now.addingTimeInterval(-86_400 * 3),
                    now: now
                )
                MonarchInboxRow(
                    "No avatar",
                    timestamp: now.addingTimeInterval(-86_400 * 400),
                    now: now,
                    showsAvatar: false
                )
            }
        },
        Fixture("count above 99, badges only") {
            MonarchInboxRow("Support queue", preview: "Escalated", unreadCount: 250) {
                MonarchBadge("SLA breached", tone: .danger)
            }
        },
        Fixture("long title and preview") {
            MonarchInboxRow(
                "Maximilian Alexander von Hohenberg-Lichtenstein and the procurement team",
                preview:
                    "A preview that is far too long to fit on one line of the inbox, so it is cut with an ellipsis",
                timestamp: now.addingTimeInterval(-86_400 * 12),
                now: now,
                isUnread: true
            )
        },
    ]
}

@Suite("MonarchInboxRow")
@MainActor
struct MonarchInboxRowTests {
    typealias Tokens = MonarchInboxRowTokens
    let labels = MonarchInboxRowLabels()
    let english = Locale(identifier: "en_GB")
    var utc: Calendar {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(identifier: "UTC") ?? .gmt
        return calendar
    }

    private func age(_ seconds: TimeInterval) -> String {
        Tokens.compactAge(now.addingTimeInterval(-seconds), now: now, labels: labels, locale: english, calendar: utc)
    }

    @Test("The age is compact up to four weeks")
    func compactAge() {
        #expect(age(0) == "now")
        #expect(age(59) == "now")
        #expect(age(5 * 60) == "5m")
        #expect(age(3 * 3_600) == "3h")
        #expect(age(2 * 86_400) == "2d")
        #expect(age(15 * 86_400) == "2w")
    }

    @Test("A future time reads as now")
    func futureAge() {
        #expect(age(-3_600) == "now")
    }

    @Test("Older times are a short date, with the year when it differs")
    func shortDate() {
        let sameYear = age(40 * 86_400)
        #expect(!sameYear.contains("2026") && !sameYear.contains("2025"))
        let otherYear = age(400 * 86_400)
        let year = utc.component(.year, from: now.addingTimeInterval(-400 * 86_400))
        #expect(otherYear.contains(String(year)))
    }

    @Test("A row is unread with the flag or a count above zero")
    func isUnread() {
        #expect(Tokens.isUnread(true, count: nil))
        #expect(Tokens.isUnread(false, count: 2))
        #expect(!Tokens.isUnread(false, count: 0))
        #expect(!Tokens.isUnread(false, count: nil))
    }

    @Test("Counts above 99 show 99+, and zero shows nothing")
    func countText() {
        #expect(Tokens.countText(nil) == nil)
        #expect(Tokens.countText(0) == nil)
        #expect(Tokens.countText(7) == "7")
        #expect(Tokens.countText(99) == "99")
        #expect(Tokens.countText(250) == "99+")
    }

    @Test("The unread state is spoken after the title")
    func spokenUnread() {
        #expect(Tokens.unreadLabel(isUnread: true, count: nil, labels: labels) == "Unread")
        #expect(Tokens.unreadLabel(isUnread: false, count: 3, labels: labels) == "3 unread")
        #expect(Tokens.unreadLabel(isUnread: false, count: nil, labels: labels) == nil)
        #expect(Tokens.titleLabel("Ana", unread: "3 unread") == "Ana, 3 unread")
        #expect(Tokens.titleLabel("Ana", unread: nil) == "Ana")
    }

    @Test("Swipe actions sit on Mail's edges, with names that follow the read state")
    func swipeActions() {
        let toggle = MonarchInboxRowSwipeAction.toggleRead {}
        let archive = MonarchInboxRowSwipeAction.archive {}
        let delete = MonarchInboxRowSwipeAction.delete {}
        #expect(Tokens.isLeading(toggle))
        #expect(!Tokens.isLeading(archive))
        #expect(!Tokens.isLeading(delete))
        #expect(Tokens.title(toggle, isUnread: true, labels: labels) == "Read")
        #expect(Tokens.title(toggle, isUnread: false, labels: labels) == "Unread")
        #expect(Tokens.title(archive, isUnread: false, labels: labels) == "Archive")
        #expect(Tokens.title(delete, isUnread: false, labels: labels) == "Delete")
        #expect(Tokens.symbol(toggle, isUnread: true) != Tokens.symbol(toggle, isUnread: false))
        #expect(Tokens.tint(delete) == \.dangerSolid)
        #expect(Tokens.tint(toggle) != Tokens.tint(archive))
    }

    @Test("Overrides replace the built-in strings")
    func overrides() {
        let custom = MonarchInboxRowLabels(
            unread: { $0.map { "\($0) ungelesen" } ?? "Ungelesen" },
            age: { unit, value in unit == .now ? "jetzt" : "\(value)" },
            markRead: "Gelesen"
        )
        #expect(Tokens.unreadLabel(isUnread: true, count: 2, labels: custom) == "2 ungelesen")
        #expect(
            Tokens.compactAge(now, now: now, labels: custom, locale: english, calendar: utc) == "jetzt"
        )
        #expect(Tokens.title(.toggleRead {}, isUnread: true, labels: custom) == "Gelesen")
    }

    @Test("The full time read by VoiceOver includes the date")
    func absoluteTime() {
        let text = Tokens.absoluteTime(now, locale: english, calendar: utc)
        #expect(text.contains(String(utc.component(.year, from: now))))
    }

    @Test("The built-in strings come from the component's strings table")
    func strings() {
        #expect(MonarchInboxRowStrings.age(.minutes, 5) == "5m")
        #expect(MonarchInboxRowStrings.age(.weeks, 2) == "2w")
        #expect(MonarchInboxRowStrings.archive == "Archive")
        #expect(MonarchInboxRowStrings.delete == "Delete")
    }

    @Test("The row is at least 44 points high")
    func minimumHeight() {
        #expect(Host.layout(MonarchInboxRow("Ana", showsAvatar: false)).height >= MonarchAccessibility.minimumHitTarget)
    }

    @Test("The time is written compact or as the full relative time of the shared rule")
    func timeStyles() {
        let now = Date(timeIntervalSince1970: 1_800_000_000)
        let calendar = Calendar(identifier: .gregorian)
        let locale = Locale(identifier: "en_US")
        let labels = MonarchInboxRowLabels()
        func text(_ style: MonarchInboxRowTimeStyle, _ seconds: TimeInterval) -> String {
            MonarchInboxRowTokens.timeText(
                now.addingTimeInterval(-seconds),
                style: style,
                now: now,
                labels: labels,
                locale: locale,
                calendar: calendar
            )
        }
        #expect(text(.compact, 300) == "5m")
        #expect(text(.relative, 300) == "5 minutes ago")
        #expect(text(.relative, 86_400) == "yesterday")
        #expect(text(.relative, 30 * 86_400) == "4 weeks ago")
    }
}
