import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchActivityTimelineFixtures: ComponentFixtures {
    static let id = "activity-timeline"

    /// 2026-03-04 15:00 UTC, fixed so nothing depends on the clock.
    static let now = Date(timeIntervalSince1970: 1_772_636_400)
    static let utc = TimeZone(identifier: "UTC") ?? .gmt

    static let events = [
        MonarchActivityTimelineEvent(
            id: "1",
            actor: MonarchActivityTimelineActor("Ana Ruiz"),
            verb: "moved",
            object: "Acme renewal",
            time: now.addingTimeInterval(-3_600),
            tone: .info,
            tags: ["Negotiation"],
            details: "Stage: Proposal → Negotiation"
        ),
        MonarchActivityTimelineEvent(
            id: "2",
            actor: MonarchActivityTimelineActor("Follow-up rule", kind: .automation),
            verb: "sent a reminder to",
            object: "Ben Okafor",
            time: now.addingTimeInterval(-7_200),
            systemImage: "bolt",
            tone: .warning
        ),
        MonarchActivityTimelineEvent(
            id: "3",
            actor: MonarchActivityTimelineActor("Ben Okafor"),
            verb: "commented on",
            object: "Acme renewal",
            time: now.addingTimeInterval(-86_400),
            body: "Legal wants the new terms by Friday. A long comment that wraps onto several lines at large sizes."
        ),
        MonarchActivityTimelineEvent(
            id: "4",
            verb: "Ticket created from email",
            time: now.addingTimeInterval(-86_400 * 40),
            tone: .success
        ),
        MonarchActivityTimelineEvent(
            id: "5",
            actor: MonarchActivityTimelineActor("Nightly import", kind: .system),
            verb: "failed for",
            object: "contacts.csv",
            time: now.addingTimeInterval(-86_400 * 400),
            tone: .danger
        ),
    ]

    static let fixtures = [
        Fixture("grouped by day with dots, tags, body, details and every tone") {
            MonarchActivityTimeline(events, now: now)
                .environment(\.timeZone, utc)
        },
        Fixture("avatar markers, compact, ungrouped, details open, load more") {
            MonarchActivityTimeline(
                events,
                isGroupedByDay: false,
                marker: .avatar,
                density: .compact,
                expandedIDs: .constant(["1"]),
                hasMore: true,
                now: now,
                onLoadMore: {}
            )
            .environment(\.timeZone, utc)
        },
        Fixture("loading, empty, failed with and without retry") {
            VStack(alignment: .leading) {
                MonarchActivityTimeline([], isLoading: true)
                MonarchActivityTimeline([], now: now)
                MonarchActivityTimeline([], isFailed: true, onRetry: {})
                MonarchActivityTimeline([], isFailed: true, labels: MonarchActivityTimelineLabels(error: "Offline"))
            }
        },
        Fixture("loading more and a failed page") {
            VStack(alignment: .leading) {
                MonarchActivityTimeline(
                    Array(events.prefix(1)),
                    hasMore: true,
                    isLoadingMore: true,
                    now: now,
                    onLoadMore: {}
                )
                MonarchActivityTimeline(
                    Array(events.prefix(1)),
                    hasMore: true,
                    isLoadMoreFailed: true,
                    now: now,
                    onLoadMore: {}
                )
            }
        },
    ]
}

@Suite("MonarchActivityTimeline")
struct MonarchActivityTimelineTests {
    private let english = Locale(identifier: "en_US")
    private let now = MonarchActivityTimelineFixtures.now
    private let events = MonarchActivityTimelineFixtures.events

    private var utcCalendar: Calendar {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = MonarchActivityTimelineFixtures.utc
        return calendar
    }

    @Test("Events are grouped by day in the calendar's time zone, keeping their order")
    func groups() {
        let groups = MonarchActivityTimelineTokens.groups(events, calendar: utcCalendar)
        #expect(groups.map { $0.events.map(\.id) } == [["1", "2"], ["3"], ["4"], ["5"]])
        // 14:00 and 13:00 UTC are one day in UTC, but 15:00 UTC crosses midnight in Tokyo (UTC+9).
        var tokyo = utcCalendar
        tokyo.timeZone = TimeZone(identifier: "Asia/Tokyo") ?? .gmt
        let late = [
            MonarchActivityTimelineEvent(id: "a", verb: "a", time: now),
            MonarchActivityTimelineEvent(id: "b", verb: "b", time: now.addingTimeInterval(-3_600)),
        ]
        #expect(MonarchActivityTimelineTokens.groups(late, calendar: utcCalendar).count == 1)
        #expect(MonarchActivityTimelineTokens.groups(late, calendar: tokyo).count == 2)
        #expect(MonarchActivityTimelineTokens.groups([], calendar: utcCalendar).isEmpty)
    }

    @Test("Days are headed Today, Yesterday, the weekday and date, and the year when it differs")
    func dayLabels() {
        let labels = MonarchActivityTimelineLabels()
        func label(_ offset: Double) -> String {
            MonarchActivityTimelineTokens.dayLabel(
                now.addingTimeInterval(offset),
                now: now,
                calendar: utcCalendar,
                locale: english,
                labels: labels
            )
        }
        #expect(label(0) == "Today")
        #expect(label(-86_400) == "Yesterday")
        #expect(label(-86_400 * 2) == "Monday, March 2")
        #expect(label(-86_400 * 400) == "Tuesday, January 28, 2025")
        let renamed = MonarchActivityTimelineTokens.dayLabel(
            now,
            now: now,
            calendar: utcCalendar,
            locale: english,
            labels: MonarchActivityTimelineLabels(today: "Hoy")
        )
        #expect(renamed == "Hoy")
    }

    @Test("Times are relative to the given now, and VoiceOver gets the absolute time")
    func times() {
        #expect(
            MonarchActivityTimelineTokens.relativeTime(
                now.addingTimeInterval(-3_600),
                now: now,
                calendar: utcCalendar,
                locale: english
            )
                == "1 hour ago"
        )
        #expect(
            MonarchActivityTimelineTokens.absoluteTime(now, calendar: utcCalendar, locale: english).hasPrefix(
                "Mar 4, 2026"
            )
        )
    }

    @Test("The sentence puts the actor and the object around the verb, in bold")
    func sentence() {
        let sentence = MonarchActivityTimelineTokens.sentence(events[0])
        #expect(String(sentence.characters) == "Ana Ruiz moved Acme renewal")
        let bold = sentence.runs.filter { $0.inlinePresentationIntent == .stronglyEmphasized }
        #expect(bold.map { String(sentence[$0.range].characters) } == ["Ana Ruiz", "Acme renewal"])
        #expect(String(MonarchActivityTimelineTokens.sentence(events[3]).characters) == "Ticket created from email")
    }

    @Test("VoiceOver reads the sentence, the time, the automated tag, the tags and the body")
    func accessibility() {
        #expect(
            MonarchActivityTimelineTokens.accessibilityLabel(events[0], time: "Mar 4", automated: "Automated")
                == "Ana Ruiz moved Acme renewal, Mar 4, Negotiation"
        )
        #expect(
            MonarchActivityTimelineTokens.accessibilityLabel(events[1], time: "Mar 4", automated: "Automated")
                == "Follow-up rule sent a reminder to Ben Okafor, Mar 4, Automated"
        )
        #expect(
            MonarchActivityTimelineTokens.accessibilityLabel(events[2], time: "Mar 3", automated: "Automated")
                .hasSuffix(
                    "Mar 3, Legal wants the new terms by Friday. A long comment that wraps onto several lines at large sizes."
                )
        )
    }

    @Test("Only automation and system actors are tagged")
    func automated() {
        #expect(!MonarchActivityTimelineTokens.isAutomated(nil))
        #expect(!MonarchActivityTimelineTokens.isAutomated(MonarchActivityTimelineActor("A")))
        #expect(MonarchActivityTimelineTokens.isAutomated(MonarchActivityTimelineActor("A", kind: .automation)))
        #expect(MonarchActivityTimelineTokens.isAutomated(MonarchActivityTimelineActor("A", kind: .system)))
    }

    @Test("Opening and closing details updates the expanded set")
    func expanded() {
        #expect(MonarchActivityTimelineTokens.toggled([], id: "1", isExpanded: true) == ["1"])
        #expect(MonarchActivityTimelineTokens.toggled(["1", "2"], id: "1", isExpanded: false) == ["2"])
        #expect(MonarchActivityTimelineTokens.toggled(["2"], id: "1", isExpanded: false) == ["2"])
    }

    @Test("Markers take their colour from the tone; neutral is the strong border")
    func colors() {
        #expect(MonarchActivityTimelineTokens.dotRole(.neutral) == \MonarchColorTokens.borderStrong)
        #expect(MonarchActivityTimelineTokens.dotRole(.success) == \MonarchColorTokens.successSolid)
        #expect(MonarchActivityTimelineTokens.dotRole(.danger) == \MonarchColorTokens.dangerSolid)
        #expect(MonarchActivityTimelineTokens.iconRoles(.warning).fill == \MonarchColorTokens.warningSubtle)
        #expect(MonarchActivityTimelineTokens.rowSpacing(.compact) < MonarchActivityTimelineTokens.rowSpacing(.regular))
    }

    @Test("The built-in strings come from the component's strings table")
    func strings() {
        #expect(MonarchActivityTimelineStrings.list == "Activity")
        #expect(MonarchActivityTimelineStrings.today == "Today")
        #expect(MonarchActivityTimelineStrings.yesterday == "Yesterday")
        #expect(MonarchActivityTimelineStrings.automated == "Automated")
        #expect(MonarchActivityTimelineStrings.details == "Details")
        #expect(MonarchActivityTimelineStrings.empty == "No activity yet")
        #expect(MonarchActivityTimelineStrings.loading == "Loading activity")
        #expect(MonarchActivityTimelineStrings.error == "Couldn’t load activity.")
    }
}
