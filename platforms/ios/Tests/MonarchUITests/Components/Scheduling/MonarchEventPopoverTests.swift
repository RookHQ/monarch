import Foundation
import SwiftUI
import Testing

@testable import MonarchUI

/// 14 November 2026, 14:30 UTC: 09:30 in New York.
private let eventPopoverStart = Date(timeIntervalSince1970: 1_794_666_600)
private let eventPopoverZone = TimeZone(identifier: "America/New_York") ?? .gmt

enum MonarchEventPopoverFixtures: ComponentFixtures {
    static let id = "event-popover"
    static let fixtures = [
        Fixture("every part, with actions") {
            MonarchEventPopover(
                "Design review",
                subtitle: "Product calendar",
                start: eventPopoverStart,
                end: eventPopoverStart.addingTimeInterval(2_700),
                status: .confirmed,
                details: [
                    MonarchEventPopoverDetail("Location", value: "Room 4", systemImage: "mappin.and.ellipse"),
                    MonarchEventPopoverDetail("Hosts", value: "Ana Silva, Jonas Weber"),
                ],
                note: "Bring the Q3 numbers.",
                timeZone: eventPopoverZone
            ) {
                MonarchButton("Open", variant: .primary, size: .sm) {}
                MonarchButton("Reschedule", size: .sm) {}
                MonarchButton("Cancel event", variant: .danger, size: .sm) {}
            }
        },
        Fixture("every status") {
            VStack(alignment: .leading) {
                ForEach(MonarchEventChipStatus.allCases, id: \.self) { status in
                    MonarchEventPopover(
                        "Stand-up",
                        start: eventPopoverStart,
                        status: status,
                        timeZone: eventPopoverZone
                    )
                }
            }
        },
        Fixture("all day over several days, custom colour") {
            MonarchEventPopover(
                "Offsite",
                start: eventPopoverStart,
                end: eventPopoverStart.addingTimeInterval(2 * 86_400),
                isAllDay: true,
                color: MonarchTheme.light.colors.data3,
                timeZone: eventPopoverZone
            )
        },
        Fixture("spans midnight, long text") {
            MonarchEventPopover(
                "Overnight data migration for the billing and invoicing systems in every region",
                subtitle: "Infrastructure on-call calendar, shared with the platform team",
                start: eventPopoverStart,
                end: eventPopoverStart.addingTimeInterval(16 * 3_600),
                status: .tentative,
                note: "Expect read-only mode for up to two hours while the database switches over.",
                timeZone: eventPopoverZone
            )
        },
        Fixture("title only") {
            MonarchEventPopover("Untitled event")
        },
    ]
}

@Suite("MonarchEventPopover")
struct MonarchEventPopoverTests {
    typealias Tokens = MonarchEventPopoverTokens
    private let labels = MonarchEventPopoverLabels()
    private let locale = Locale(identifier: "en_US")

    private func when(
        start: Date = eventPopoverStart,
        end: Date?,
        isAllDay: Bool = false,
        zone: TimeZone = eventPopoverZone
    ) -> Tokens.When {
        Tokens.when(start: start, end: end, isAllDay: isAllDay, timeZone: zone, locale: locale, allDayLabel: "All day")
    }

    @Test("Statuses take the web's tones")
    func tones() {
        #expect(Tokens.tone(.confirmed) == .success)
        #expect(Tokens.tone(.tentative) == .warning)
        #expect(Tokens.tone(.cancelled) == .neutral)
        #expect(Tokens.tone(.declined) == .danger)
    }

    @Test("Status names reuse the event chip's and can be replaced")
    func statusNames() {
        #expect(Tokens.statusName(.tentative, labels: labels) == "Tentative")
        #expect(Tokens.statusName(.declined, labels: labels) == "Declined")
        #expect(Tokens.statusName(.declined, labels: MonarchEventPopoverLabels(status: "Abgelehnt")) == "Abgelehnt")
    }

    @Test("Cancelled and declined titles are struck through")
    func struckThrough() {
        #expect(Tokens.isStruckThrough(.cancelled))
        #expect(Tokens.isStruckThrough(.declined))
        #expect(Tokens.isStruckThrough(.confirmed) == false)
        #expect(Tokens.isStruckThrough(nil) == false)
    }

    @Test("The built-in strings resolve and can be replaced")
    func strings() {
        #expect(Tokens.timeLabel(labels: labels) == "Time")
        #expect(Tokens.allDayLabel(labels: labels) == "All day")
        let custom = MonarchEventPopoverLabels(time: "Zeit", allDay: "Ganztägig")
        #expect(Tokens.timeLabel(labels: custom) == "Zeit")
        #expect(Tokens.allDayLabel(labels: custom) == "Ganztägig")
    }

    @Test("A same-day event shows the day, then both times and the zone")
    func sameDay() {
        let result = when(end: eventPopoverStart.addingTimeInterval(2_700))
        #expect(result.date.contains("Nov"))
        #expect(result.date.contains("14"))
        #expect(result.date.contains("2026"))
        #expect(result.date.contains("Sat"))
        #expect(result.time.contains("9:30"))
        #expect(result.time.contains("10:15"))
        #expect(result.time.contains(" – "))
        #expect(result.time.hasSuffix("EST"))
    }

    @Test("Times follow the given zone")
    func zones() {
        let auckland = TimeZone(identifier: "Pacific/Auckland") ?? .gmt
        let result = when(end: nil, zone: auckland)
        #expect(result.date.contains("15"))
        #expect(result.time.contains("3:30"))
        #expect(!result.time.contains(" – "))
    }

    @Test("An event that ends on another day shows the start day and the end day")
    func spansDays() {
        let result = when(end: eventPopoverStart.addingTimeInterval(16 * 3_600))
        #expect(result.date.hasSuffix(" –"))
        #expect(result.date.contains("9:30"))
        #expect(result.time.contains("15"))
        #expect(result.time.contains("1:30"))
    }

    @Test("All-day events show the day or the range and All day")
    func allDay() {
        let single = when(end: eventPopoverStart.addingTimeInterval(3_600), isAllDay: true)
        #expect(single.time == "All day")
        #expect(!single.date.contains(" – "))
        let range = when(end: eventPopoverStart.addingTimeInterval(2 * 86_400), isAllDay: true)
        #expect(range.time == "All day")
        #expect(range.date.contains(" – "))
        #expect(range.date.contains("16"))
    }

    @Test("An end before the start is ignored")
    func endBeforeStart() {
        let result = when(end: eventPopoverStart.addingTimeInterval(-3_600))
        #expect(result == when(end: nil))
    }

    @Test("The time row is read as its label, then the date and the time")
    func timeAccessibility() {
        let spoken = Tokens.timeAccessibility(Tokens.When(date: "Sat, Nov 14, 2026", time: "All day"), labels: labels)
        #expect(spoken.label == "Time")
        #expect(spoken.value == "Sat, Nov 14, 2026, All day")
    }
}
