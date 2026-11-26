import Foundation
import SwiftUI
import Testing

@testable import MonarchUI

private let now = Date(timeIntervalSinceReferenceDate: 800_000_000)
private var ana: MonarchMessageRowAuthor { MonarchMessageRowAuthor("Ana Lopez", seed: "u_1") }
private let reactions = [
    MonarchReactionBarItem("👍", count: 3, isReacted: true, label: "thumbs up", reactedBy: "You, Ben and Chloé"),
    MonarchReactionBarItem("🎉", count: 1),
]

enum MonarchMessageRowFixtures: ComponentFixtures {
    static let id = "message-row"
    static let fixtures = [
        Fixture("sent with reactions, replies and actions") {
            MonarchMessageRow(
                ana,
                time: now.addingTimeInterval(-480),
                now: now,
                reactions: reactions,
                replyCount: 4,
                onReactionToggle: { _, _ in },
                onOpenThread: {}
            ) {
                Text("The release notes are ready for review. Comments by Thursday, please.")
            } actions: {
                Button("Reply", systemImage: "arrowshape.turn.up.left") {}
                Button("Delete", systemImage: "trash", role: .destructive) {}
            }
        },
        Fixture("grouped, edited with history, clock time") {
            VStack(spacing: 0) {
                MonarchMessageRow(ana, time: now.addingTimeInterval(-600), now: now, timeStyle: .clock) {
                    Text("First message")
                }
                MonarchMessageRow(
                    ana,
                    time: now.addingTimeInterval(-540),
                    now: now,
                    timeStyle: .clock,
                    isGrouped: true,
                    isEdited: true,
                    onShowEdits: {}
                ) {
                    Text("A follow-up that was edited")
                }
                MonarchMessageRow(ana, time: now, now: now, isEdited: true) { Text("Edited, no history") }
            }
        },
        Fixture("pending and failed") {
            VStack(spacing: 0) {
                MonarchMessageRow(ana, time: now, now: now, status: .pending) { Text("On its way") }
                MonarchMessageRow(ana, time: now, now: now, status: .failed, onRetry: {}, onDiscard: {}) {
                    Text("Shipping it now")
                }
                MonarchMessageRow(
                    ana,
                    time: now,
                    now: now,
                    status: .failed,
                    errorMessage: "You are offline. The message will be sent when you reconnect."
                ) {
                    Text("No buttons")
                }
            }
        },
        Fixture("internal, highlighted, deleted, read-only reactions") {
            VStack(spacing: 0) {
                MonarchMessageRow(
                    MonarchMessageRowAuthor("Chloé Martin", image: Image(systemName: "person.fill")),
                    time: now,
                    now: now,
                    variant: .internal,
                    tags: ["Internal", "Admin"]
                ) {
                    Text("Customer is on the enterprise plan.")
                }
                MonarchMessageRow(ana, time: now, now: now, isHighlighted: true) { Text("Linked message") }
                MonarchMessageRow(ana, time: now, now: now, reactions: reactions, replyCount: 1, isDeleted: true) {
                    Text("Hidden")
                }
                MonarchMessageRow(ana, time: now, now: now, reactions: reactions, replyCount: 1) {
                    Text("Read-only reactions and replies")
                }
            }
        },
        Fixture("system line") {
            MonarchMessageRow(ana, time: now.addingTimeInterval(-86_400 * 2), now: now, variant: .system) {
                Text("Ana Lopez renamed the channel to #launch-planning-for-the-autumn-release")
            }
        },
        Fixture("long author and body") {
            MonarchMessageRow(
                MonarchMessageRowAuthor("Maximilian Alexander von Hohenberg-Lichtenstein"),
                time: now,
                now: now,
                tags: ["Guest"]
            ) {
                Text(String(repeating: "A long message that wraps over several lines. ", count: 6))
            }
        },
    ]
}

@Suite("MonarchMessageRow")
@MainActor
struct MonarchMessageRowTests {
    typealias Tokens = MonarchMessageRowTokens
    let labels = MonarchMessageRowLabels()
    let english = Locale(identifier: "en_GB")
    var utc: Calendar {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(identifier: "UTC") ?? .gmt
        calendar.locale = Locale(identifier: "en_GB")
        return calendar
    }

    @Test("A highlight wins over the internal tint; only internal notes have a bar")
    func background() {
        #expect(Tokens.background(variant: .standard, isHighlighted: false) == nil)
        #expect(Tokens.background(variant: .internal, isHighlighted: false) == \.warningSubtle)
        #expect(Tokens.background(variant: .internal, isHighlighted: true) == \.accentSubtle)
        #expect(Tokens.background(variant: .system, isHighlighted: true) == \.accentSubtle)
        #expect(Tokens.leadingBar(.internal) == \.warningBorder)
        #expect(Tokens.leadingBar(.standard) == nil)
        #expect(Tokens.leadingBar(.system) == nil)
    }

    @Test("The body is muted while pending and when deleted")
    func bodyForeground() {
        #expect(Tokens.bodyForeground(status: .sent, isDeleted: false) == \.textDefault)
        #expect(Tokens.bodyForeground(status: .failed, isDeleted: false) == \.textDefault)
        #expect(Tokens.bodyForeground(status: .pending, isDeleted: false) == \.textMuted)
        #expect(Tokens.bodyForeground(status: .sent, isDeleted: true) == \.textMuted)
    }

    @Test("Actions are offered only on a delivered, not deleted message")
    func showsActions() {
        #expect(Tokens.showsActions(status: .sent, isDeleted: false, variant: .standard))
        #expect(Tokens.showsActions(status: .sent, isDeleted: false, variant: .internal))
        #expect(!Tokens.showsActions(status: .pending, isDeleted: false, variant: .standard))
        #expect(!Tokens.showsActions(status: .failed, isDeleted: false, variant: .standard))
        #expect(!Tokens.showsActions(status: .sent, isDeleted: true, variant: .standard))
        #expect(!Tokens.showsActions(status: .sent, isDeleted: false, variant: .system))
    }

    @Test("Reactions are hidden when deleted or when every count is zero")
    func showsReactions() {
        #expect(Tokens.showsReactions(reactions, isDeleted: false))
        #expect(!Tokens.showsReactions(reactions, isDeleted: true))
        #expect(!Tokens.showsReactions([], isDeleted: false))
        #expect(!Tokens.showsReactions([MonarchReactionBarItem("👍", count: 0)], isDeleted: false))
    }

    @Test("The reply count is singular, plural, or absent")
    func replies() {
        #expect(Tokens.repliesText(0, labels: labels) == nil)
        #expect(Tokens.repliesText(1, labels: labels) == "1 reply")
        #expect(Tokens.repliesText(4, labels: labels) == "4 replies")
    }

    @Test("A failed message shows its error or the built-in text")
    func errorText() {
        #expect(Tokens.errorText(nil, labels: labels) == "Not sent")
        #expect(Tokens.errorText("", labels: labels) == "Not sent")
        #expect(Tokens.errorText("Offline", labels: labels) == "Offline")
    }

    @Test("The relative time is named and never in the future")
    func relativeTime() {
        let fiveMinutes = Tokens.relativeTime(now.addingTimeInterval(-300), now: now, locale: english, calendar: utc)
        #expect(fiveMinutes == "5 minutes ago")
        #expect(Tokens.relativeTime(now.addingTimeInterval(3_600), now: now, locale: english, calendar: utc) == "now")
        #expect(
            Tokens.timeText(now.addingTimeInterval(-300), style: .relative, now: now, locale: english, calendar: utc)
                == fiveMinutes
        )
    }

    @Test("The clock time follows the locale and the calendar's time zone")
    func clockTime() {
        let date = Date(timeIntervalSince1970: 1_772_453_100)  // 2026-03-02 12:05 UTC
        #expect(Tokens.clockTime(date, locale: english, calendar: utc) == "12:05")
        #expect(Tokens.timeText(date, style: .clock, now: now, locale: english, calendar: utc) == "12:05")
        let full = Tokens.absoluteTime(date, locale: english, calendar: utc)
        #expect(full.contains("2026") && full.contains("March") && full.contains("12:05"))
    }

    @Test("A grouped message is read with its author and time")
    func byline() {
        #expect(
            Tokens.byline(author: "Ana Lopez", time: "Monday, 2 March 2026 at 12:05")
                == "Ana Lopez, Monday, 2 March 2026 at 12:05"
        )
    }

    @Test("Overrides replace the built-in strings")
    func overrides() {
        let custom = MonarchMessageRowLabels(failed: "Nicht gesendet", replies: { "\($0) Antworten" })
        #expect(Tokens.errorText(nil, labels: custom) == "Nicht gesendet")
        #expect(Tokens.repliesText(2, labels: custom) == "2 Antworten")
    }

    @Test("The built-in strings come from the component's strings table")
    func strings() {
        #expect(MonarchMessageRowStrings.sending == "Sending…")
        #expect(MonarchMessageRowStrings.retry == "Retry")
        #expect(MonarchMessageRowStrings.discard == "Discard")
        #expect(MonarchMessageRowStrings.edited == "(edited)")
        #expect(MonarchMessageRowStrings.editHistory == "Show edit history")
        #expect(MonarchMessageRowStrings.deleted == "This message was deleted.")
    }

    #if os(iOS)
        @Test("The thread button is at least 44 points high")
        func threadTarget() {
            let size = Host.layout(
                MonarchMessageRow(ana, time: now, now: now, replyCount: 2, onOpenThread: {}) { Text("Hi") }
            )
            #expect(size.height >= MonarchAccessibility.minimumHitTarget * 2)
        }
    #endif
}
