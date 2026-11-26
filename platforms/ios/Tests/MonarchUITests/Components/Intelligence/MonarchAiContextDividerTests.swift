import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchAiContextDividerFixtures: ComponentFixtures {
    static let id = "ai-context-divider"
    static let fixtures = [
        Fixture("day, today and an older date") {
            VStack {
                MonarchAiContextDivider(.day, date: Date())
                MonarchAiContextDivider(.day, date: Date(timeIntervalSince1970: 1_700_000_000))
                MonarchAiContextDivider(.day)
            }
        },
        Fixture("context reset") {
            MonarchAiContextDivider(.contextReset)
        },
        Fixture("summarized without controls") {
            MonarchAiContextDivider(.summarized)
        },
        Fixture("summarized with summary and expand") {
            MonarchAiContextDivider(
                .summarized,
                summary: "The team agreed to ship the report on Friday and to review the numbers on Monday."
            ) {}
        },
        Fixture("summarized with expand only") {
            MonarchAiContextDivider(.summarized, expandTitle: "Load 42 earlier messages") {}
        },
        Fixture("long custom label") {
            MonarchAiContextDivider(
                .contextReset,
                label: "The conversation was restarted because the workspace changed",
                detail: "Nothing above this line is sent to the assistant, including files and tool results."
            )
        },
    ]
}

@Suite("MonarchAiContextDivider")
struct MonarchAiContextDividerTests {
    private static let locale = Locale(identifier: "en_US")
    private static var calendar: Calendar {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(secondsFromGMT: 0) ?? .gmt
        calendar.locale = locale
        return calendar
    }

    private static func date(_ year: Int, _ month: Int, _ day: Int, hour: Int = 12) -> Date {
        calendar.date(from: DateComponents(year: year, month: month, day: day, hour: hour)) ?? .distantPast
    }

    private static let now = date(2025, 3, 10)

    private func dayLabel(_ date: Date) -> String {
        MonarchAiContextDividerTokens.dayLabel(for: date, now: Self.now, calendar: Self.calendar, locale: Self.locale)
    }

    @Test("A day is Today, Yesterday, a weekday and date, or a full date")
    func dayLabels() {
        #expect(dayLabel(Self.date(2025, 3, 10, hour: 1)) == "Today")
        #expect(dayLabel(Self.date(2025, 3, 9, hour: 23)) == "Yesterday")
        #expect(dayLabel(Self.date(2025, 3, 3)) == "Monday, March 3")
        #expect(dayLabel(Self.date(2024, 3, 3)) == "Sunday, March 3, 2024")
        // Tomorrow is not a relative name: it is a date like any other.
        #expect(dayLabel(Self.date(2025, 3, 11)) == "Tuesday, March 11")
    }

    @Test("The day is counted in the calendar's time zone")
    func dayLabelFollowsTimeZone() {
        var tokyo = Self.calendar
        tokyo.timeZone = TimeZone(identifier: "Asia/Tokyo") ?? .gmt
        // 20:00 UTC on the 9th is already the 10th in Tokyo, the same day as noon UTC on the 10th.
        let evening = Self.date(2025, 3, 9, hour: 20)
        #expect(dayLabel(evening) == "Yesterday")
        #expect(
            MonarchAiContextDividerTokens.dayLabel(for: evening, now: Self.now, calendar: tokyo, locale: Self.locale)
                == "Today"
        )
    }

    @Test("The label comes from the kind, the date or the override")
    func label() {
        func label(_ kind: MonarchAiContextDividerKind, date: Date? = nil, override: String? = nil) -> String {
            MonarchAiContextDividerTokens.label(
                kind: kind,
                date: date,
                now: Self.now,
                calendar: Self.calendar,
                locale: Self.locale,
                override: override
            )
        }
        #expect(label(.day, date: Self.now) == "Today")
        #expect(label(.day).isEmpty)
        #expect(label(.contextReset) == "Context reset")
        #expect(label(.summarized) == "Earlier messages were summarized")
        #expect(label(.day, date: Self.now, override: "Earlier") == "Earlier")
    }

    @Test("Each kind has its own line, and a reset is dashed")
    func lines() {
        #expect(MonarchAiContextDividerTokens.lineRole(.day) == \MonarchColorTokens.borderSubtle)
        #expect(MonarchAiContextDividerTokens.lineRole(.contextReset) == \MonarchColorTokens.borderDefault)
        #expect(MonarchAiContextDividerTokens.lineRole(.summarized) == \MonarchColorTokens.aiBorder)
        #expect(MonarchAiContextDividerTokens.labelRole(.day) == \MonarchColorTokens.textMuted)
        #expect(MonarchAiContextDividerTokens.labelRole(.contextReset) == \MonarchColorTokens.textMuted)
        #expect(MonarchAiContextDividerTokens.labelRole(.summarized) == \MonarchColorTokens.aiStrong)
        #expect(MonarchAiContextDividerKind.allCases.filter(MonarchAiContextDividerTokens.isDashed) == [.contextReset])
    }

    @Test("Only a summarized divider with a summary or an expand action has controls")
    func interactive() {
        for kind in MonarchAiContextDividerKind.allCases {
            #expect(!MonarchAiContextDividerTokens.isInteractive(kind: kind, hasSummary: false, hasExpand: false))
        }
        #expect(MonarchAiContextDividerTokens.isInteractive(kind: .summarized, hasSummary: true, hasExpand: false))
        #expect(MonarchAiContextDividerTokens.isInteractive(kind: .summarized, hasSummary: false, hasExpand: true))
        #expect(!MonarchAiContextDividerTokens.isInteractive(kind: .day, hasSummary: true, hasExpand: true))
        #expect(!MonarchAiContextDividerTokens.isInteractive(kind: .contextReset, hasSummary: true, hasExpand: true))
    }

    @Test("VoiceOver reads the label, then the description")
    func accessibilityLabel() {
        #expect(MonarchAiContextDividerTokens.accessibilityLabel(label: "Today", detail: nil) == "Today")
        #expect(MonarchAiContextDividerTokens.accessibilityLabel(label: "Today", detail: "") == "Today")
        #expect(
            MonarchAiContextDividerTokens.accessibilityLabel(label: "Context reset", detail: "Nothing above is seen.")
                == "Context reset. Nothing above is seen."
        )
        #expect(MonarchAiContextDividerTokens.accessibilityLabel(label: "", detail: "Only this") == "Only this")
        #expect(
            MonarchAiContextDividerTokens.summaryAccessibilityLabel(prefix: "AI-generated summary:", summary: "Done.")
                == "AI-generated summary: Done."
        )
        #expect(MonarchAiContextDividerTokens.summaryAccessibilityLabel(prefix: "", summary: "Done.") == "Done.")
    }

    @Test("The built-in strings come from the component's strings table")
    func strings() {
        #expect(MonarchAiContextDividerStrings.contextReset == "Context reset")
        #expect(
            MonarchAiContextDividerStrings.contextResetDescription
                == "The assistant does not see the messages above this line."
        )
        #expect(MonarchAiContextDividerStrings.summarized == "Earlier messages were summarized")
        #expect(MonarchAiContextDividerStrings.showSummary == "Show summary")
        #expect(MonarchAiContextDividerStrings.summaryPrefix == "AI-generated summary:")
        #expect(MonarchAiContextDividerStrings.expand == "Show earlier messages")
    }

    #if os(iOS)
        @Test("The controls of a summarized divider are at least 44 points high")
        @MainActor
        func controlsHitTarget() {
            let plain = Host.layout(MonarchAiContextDivider(.summarized))
            let withControls = Host.layout(MonarchAiContextDivider(.summarized, summary: "Summary") {})
            #expect(withControls.height >= plain.height + 2 * MonarchAccessibility.minimumHitTarget)
        }
    #endif
}
