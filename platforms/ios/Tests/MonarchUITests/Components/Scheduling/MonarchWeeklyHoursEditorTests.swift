import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

private typealias Interval = MonarchWeeklyHoursInterval
private typealias Tokens = MonarchWeeklyHoursEditorTokens

private enum WeeklyHoursSamples {
    static let week = (1...5).map { Interval(day: $0, start: 540, end: 1020) }
    static let split = [
        Interval(day: 1, start: 540, end: 720),
        Interval(day: 1, start: 780, end: 1020),
        Interval(day: 3, start: 600, end: 960),
    ]
    static let invalid = [
        Interval(day: 1, start: -15, end: 600),
        Interval(day: 2, start: 600, end: 540),
        Interval(day: 3, start: 540, end: 720),
        Interval(day: 3, start: 660, end: 900),
    ]
}

enum MonarchWeeklyHoursEditorFixtures: ComponentFixtures {
    static let id = "weekly-hours-editor"
    static let fixtures = [
        Fixture("working week with a time zone line") {
            MonarchWeeklyHoursEditor(value: .constant(WeeklyHoursSamples.week), firstWeekday: 2) {
                Text(verbatim: "Times in Europe/London")
            }
        },
        Fixture("split days, Sunday first, 24-hour clock") {
            MonarchWeeklyHoursEditor(
                value: .constant(WeeklyHoursSamples.split + [Interval(day: 6, start: 1320, end: 1440)]),
                firstWeekday: 1,
                uses24HourClock: true,
                step: 30
            )
        },
        Fixture("every problem shown") {
            MonarchWeeklyHoursEditor(value: .constant(WeeklyHoursSamples.invalid), firstWeekday: 2)
        },
        Fixture("at the interval limit, off-step time") {
            MonarchWeeklyHoursEditor(
                value: .constant([Interval(day: 2, start: 547, end: 600), Interval(day: 2, start: 660, end: 720)]),
                firstWeekday: 2,
                uses24HourClock: false,
                maxIntervals: 2
            )
        },
        Fixture("read-only, midnight end") {
            MonarchWeeklyHoursEditor(
                value: .constant(WeeklyHoursSamples.split + [Interval(day: 5, start: 1320, end: 1440)]),
                firstWeekday: 2,
                uses24HourClock: false,
                isReadOnly: true
            )
        },
        Fixture("empty and disabled, long labels") {
            MonarchWeeklyHoursEditor(
                value: .constant([]),
                firstWeekday: 2,
                labels: MonarchWeeklyHoursEditorLabels(
                    title: "Hours when the support desk answers phone calls and live chat",
                    unavailable: "Closed all day, messages are answered the next working day"
                )
            )
            .disabled(true)
        },
    ]
}

@Suite("MonarchWeeklyHoursEditor")
@MainActor
struct MonarchWeeklyHoursEditorTests {
    private let text = MonarchWeeklyHoursEditorText(labels: MonarchWeeklyHoursEditorLabels())
    private let us = Locale(identifier: "en_US")
    private let gb = Locale(identifier: "en_GB")

    @Test("Validation finds the first problem of each day")
    func validation() {
        #expect(Interval.validate(WeeklyHoursSamples.week).isEmpty)
        #expect(Interval.validate(WeeklyHoursSamples.split).isEmpty)
        #expect(Interval.validate(WeeklyHoursSamples.invalid) == [1: .range, 2: .order, 3: .overlap])
        #expect(Interval.validate([Interval(day: 4, start: 600, end: 1441)]) == [4: .range])
        #expect(Interval.validate([Interval(day: 4, start: 600, end: 600)]) == [4: .order])
        // Touching intervals do not overlap: the end is exclusive.
        #expect(
            Interval.validate([Interval(day: 4, start: 540, end: 720), Interval(day: 4, start: 720, end: 800)]).isEmpty
        )
        // Order is checked before overlap, regardless of the order arranged.
        let both = [Interval(day: 5, start: 600, end: 900), Interval(day: 5, start: 700, end: 650)]
        #expect(Interval.validate(both) == [5: .order])
    }

    @Test("The step stays between 1 and 60 and falls back to 15")
    func step() {
        #expect(Tokens.step(15) == 15)
        #expect(Tokens.step(1) == 1)
        #expect(Tokens.step(90) == 60)
        #expect(Tokens.step(0) == 15)
        #expect(Tokens.step(-5) == 15)
    }

    @Test("Days are ordered from the first weekday and map between Calendar and ISO numbers")
    func order() {
        #expect(Tokens.order(firstWeekday: 2) == [1, 2, 3, 4, 5, 6, 7])
        #expect(Tokens.order(firstWeekday: 1) == [7, 1, 2, 3, 4, 5, 6])
        #expect(Tokens.order(firstWeekday: 7) == [6, 7, 1, 2, 3, 4, 5])
        #expect(Tokens.order(firstWeekday: 0) == [7, 1, 2, 3, 4, 5, 6])
        for iso in 1...7 {
            #expect(Tokens.isoDay(calendarWeekday: Tokens.calendarWeekday(isoDay: iso)) == iso)
        }
    }

    @Test("Replacing a day keeps the others, orders by weekday and drops unknown days")
    func withDay() {
        let value = [
            Interval(day: 3, start: 600, end: 700), Interval(day: 9, start: 0, end: 60),
            Interval(day: 1, start: 540, end: 600),
        ]
        let next = Tokens.withDay(value, day: 2, ranges: [Interval(day: 0, start: 60, end: 120)])
        #expect(
            next == [
                Interval(day: 1, start: 540, end: 600),
                Interval(day: 2, start: 60, end: 120),
                Interval(day: 3, start: 600, end: 700),
            ]
        )
        #expect(Tokens.withDay(value, day: 3, ranges: []) == [Interval(day: 1, start: 540, end: 600)])
    }

    @Test("Copying a day gives every day its intervals")
    func copy() {
        let copied = Tokens.copiedToAll(WeeklyHoursSamples.split, from: 1)
        #expect(copied.count == 14)
        for day in 1...7 {
            #expect(Tokens.dayOf(copied, day).map(\.start) == [540, 780])
        }
        #expect(Tokens.copiedToAll(WeeklyHoursSamples.split, from: 2).isEmpty)
    }

    @Test("Switching on restores remembered intervals, else the default")
    func switchedOn() {
        let remembered = [Interval(day: 2, start: 600, end: 660)]
        #expect(Tokens.switchedOn(day: 2, remembered: remembered, defaultStart: 540, defaultEnd: 1020) == remembered)
        #expect(
            Tokens.switchedOn(day: 2, remembered: [], defaultStart: 540, defaultEnd: 1020) == [
                Interval(day: 2, start: 540, end: 1020)
            ]
        )
        #expect(
            Tokens.switchedOn(day: 4, remembered: nil, defaultStart: 480, defaultEnd: 960) == [
                Interval(day: 4, start: 480, end: 960)
            ]
        )
    }

    @Test("Adding appends an hour-long interval an hour after the last, within the day")
    func nextInterval() {
        #expect(Tokens.nextInterval(after: [], day: 1, step: 15) == Interval(day: 1, start: 540, end: 1020))
        #expect(
            Tokens.nextInterval(after: [Interval(day: 1, start: 540, end: 720)], day: 1, step: 15)
                == Interval(day: 1, start: 780, end: 840)
        )
        #expect(
            Tokens.nextInterval(after: [Interval(day: 1, start: 1200, end: 1440)], day: 1, step: 15)
                == Interval(day: 1, start: 1425, end: 1440)
        )
        #expect(Tokens.canAdd(count: 5, maxIntervals: 6))
        #expect(!Tokens.canAdd(count: 6, maxIntervals: 6))
    }

    @Test("Time options follow the step and keep an off-step value")
    func options() {
        let starts = Tokens.startOptions(step: 60, current: 540)
        #expect(starts.first == 0)
        #expect(starts.last == 1380)
        #expect(starts.count == 24)
        let ends = Tokens.endOptions(step: 60, current: 1440)
        #expect(ends.first == 60)
        #expect(ends.last == 1440)
        #expect(Tokens.startOptions(step: 15, current: 547).contains(547))
        #expect(Tokens.startOptions(step: 15, current: 547) == Tokens.startOptions(step: 15, current: 547).sorted())
        #expect(Tokens.startOptions(step: 15, current: 540).count == 96)
    }

    @Test("Only new or changed problems are announced")
    func newErrors() {
        #expect(Tokens.newErrors(from: [:], to: [2: .order]) == [2])
        #expect(Tokens.newErrors(from: [2: .order], to: [2: .order]).isEmpty)
        #expect(Tokens.newErrors(from: [2: .order], to: [2: .overlap, 5: .range]) == [2, 5])
        #expect(Tokens.newErrors(from: [2: .order], to: [:]).isEmpty)
    }

    @Test("Weekday names and times follow the locale; the end of the day reads Midnight or 24:00")
    func formats() {
        let twelve = MonarchWeeklyHoursEditorFormats(locale: us, uses24HourClock: false, midnight: text.midnight)
        #expect(twelve.day(1) == "Monday")
        #expect(twelve.day(7) == "Sunday")
        #expect(twelve.time(540).hasPrefix("9:00"))
        #expect(twelve.time(540).contains("AM"))
        #expect(twelve.time(1440) == "Midnight")
        let twentyFour = MonarchWeeklyHoursEditorFormats(locale: us, uses24HourClock: true, midnight: text.midnight)
        #expect(twentyFour.time(540) == "09:00")
        #expect(twentyFour.time(0) == "00:00")
        #expect(twentyFour.time(1440) == "24:00")
        let german = MonarchWeeklyHoursEditorFormats(
            locale: Locale(identifier: "de_DE"),
            uses24HourClock: nil,
            midnight: ""
        )
        #expect(german.day(3) == "Mittwoch")
        #expect(german.is24Hour)
        #expect(!MonarchWeeklyHoursEditorFormats(locale: us, uses24HourClock: nil, midnight: "").is24Hour)
        #expect(MonarchWeeklyHoursEditorFormats(locale: gb, uses24HourClock: nil, midnight: "").is24Hour)
    }

    @Test("A day's hours as text")
    func summary() {
        let formats = MonarchWeeklyHoursEditorFormats(locale: us, uses24HourClock: true, midnight: text.midnight)
        #expect(text.summary([], formats: formats) == "Unavailable")
        #expect(text.summary([Interval(day: 1, start: 540, end: 1020)], formats: formats) == "09:00 – 17:00")
        let two = text.summary(Array(WeeklyHoursSamples.split.prefix(2)), formats: formats)
        #expect(two.hasPrefix("09:00 – 12:00"))
        #expect(two.hasSuffix("13:00 – 17:00"))
    }

    @Test("Spoken names carry the day and position")
    func spokenNames() {
        #expect(text.interval("Monday", 2) == "Monday interval 2")
        #expect(text.addItem("Monday") == "Add interval to Monday")
        #expect(text.removeItem("Monday", 1) == "Remove Monday interval 1")
        #expect(text.copyToAllItem("Friday") == "Copy to all days from Friday")
        #expect(text.copied("Friday") == "Friday hours copied to every day.")
        #expect(text.range("9:00 AM", "5:00 PM") == "9:00 AM – 5:00 PM")
        // Voice Control: the spoken name keeps the visible title.
        #expect(text.addItem("Monday").contains(text.add))
        #expect(text.copyToAllItem("Monday").contains(text.copyToAll))
    }

    @Test("The built-in strings resolve")
    func strings() {
        #expect(text.title == "Weekly hours")
        #expect(text.unavailable == "Unavailable")
        #expect(text.start == "Start")
        #expect(text.end == "End")
        #expect(text.add == "Add interval")
        #expect(text.remove == "Remove")
        #expect(text.copyToAll == "Copy to all")
        #expect(text.midnight == "Midnight")
        #expect(text.error(.range) == "Times must fall within the day")
        #expect(text.error(.order) == "Each interval must end after it starts")
        #expect(text.error(.overlap) == "Intervals must not overlap")
    }

    @Test("Replacements win over the built-in strings")
    func overrides() {
        let custom = MonarchWeeklyHoursEditorText(
            labels: MonarchWeeklyHoursEditorLabels(
                title: "Opening hours",
                removeItem: { day, position in "Delete \(day) slot \(position)" },
                range: { start, end in "\(start) to \(end)" },
                overlapError: "Slots overlap"
            )
        )
        #expect(custom.title == "Opening hours")
        #expect(custom.removeItem("Monday", 3) == "Delete Monday slot 3")
        #expect(custom.range("9", "5") == "9 to 5")
        #expect(custom.error(.overlap) == "Slots overlap")
        #expect(custom.error(.order) == "Each interval must end after it starts")
        #expect(custom.unavailable == "Unavailable")
    }
}
