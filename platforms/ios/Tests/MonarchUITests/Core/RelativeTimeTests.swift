import Foundation
import SwiftUI
import Testing

@testable import MonarchUI

/// The one relative-time rule (`consolidation/relative-time-rule.md`), the
/// web `formatRelativeTime` rule. The cases mirror Android's
/// `MonarchRelativeTimeTest.kt`, which mirrors the web tests.
@Suite("Relative time")
struct RelativeTimeTests {
    private static let now = Date(timeIntervalSince1970: 1_800_000_000)
    private static let en = Locale(identifier: "en_US")
    private static let minute: TimeInterval = 60
    private static let hour: TimeInterval = 3600
    private static let day: TimeInterval = 86400

    private static func at(_ offset: TimeInterval) -> Date { now.addingTimeInterval(offset) }

    private static func format(_ offset: TimeInterval, isNumeric: Bool = false) -> String {
        MonarchRelativeTime.string(at(offset), now: now, locale: en, allowsFuture: true, isNumeric: isNumeric)
    }

    private static func parts(_ offset: TimeInterval) -> MonarchRelativeTime.Parts {
        MonarchRelativeTime.parts(at(offset), now: now)
    }

    private typealias Parts = MonarchRelativeTime.Parts

    @Test("Picks the largest unit that fits after rounding, as the web does")
    func units() {
        #expect(Self.parts(-30) == Parts(value: -30, unit: .second))
        #expect(Self.parts(-5 * Self.minute) == Parts(value: -5, unit: .minute))
        #expect(Self.parts(-59.6 * Self.minute) == Parts(value: -1, unit: .hour))
        #expect(Self.parts(2 * Self.day) == Parts(value: 2, unit: .day))
        #expect(Self.parts(-14 * Self.day) == Parts(value: -2, unit: .week))
        #expect(Self.parts(-400 * Self.day) == Parts(value: -1, unit: .year))
    }

    @Test("The rollovers are the web ones, with Math.round halves")
    func rollovers() {
        // -59.5 s rounds to -59 (still seconds), -59.6 s to -60 (a minute), +59.5 s to 60 (a minute).
        #expect(Self.parts(-59.4) == Parts(value: -59, unit: .second))
        #expect(Self.parts(-59.5) == Parts(value: -59, unit: .second))
        #expect(Self.parts(-59.6) == Parts(value: -1, unit: .minute))
        #expect(Self.parts(59.5) == Parts(value: 1, unit: .minute))
        #expect(Self.parts(-23 * Self.hour) == Parts(value: -23, unit: .hour))
        #expect(Self.parts(-23.6 * Self.hour) == Parts(value: -1, unit: .day))
        #expect(Self.parts(-6 * Self.day) == Parts(value: -6, unit: .day))
        #expect(Self.parts(-6.6 * Self.day) == Parts(value: -1, unit: .week))
        #expect(Self.parts(-28 * Self.day) == Parts(value: -4, unit: .week))
        #expect(Self.parts(-30 * Self.day) == Parts(value: -4, unit: .week))
        #expect(Self.parts(-31 * Self.day) == Parts(value: -4, unit: .week))
        #expect(Self.parts(-32 * Self.day) == Parts(value: -1, unit: .month))
        #expect(Self.parts(-335 * Self.day) == Parts(value: -11, unit: .month))
        #expect(Self.parts(-350 * Self.day) == Parts(value: -11, unit: .month))
        #expect(Self.parts(-351 * Self.day) == Parts(value: -1, unit: .year))
        #expect(Self.parts(-3653 * Self.day) == Parts(value: -10, unit: .year))
    }

    @Test("Halves round towards positive infinity")
    func halves() {
        #expect(Self.parts(-2.5) == Parts(value: -2, unit: .second))
        #expect(Self.parts(2.5) == Parts(value: 3, unit: .second))
        #expect(Self.parts(-0.4) == Parts(value: 0, unit: .second))
        #expect(Self.parts(-0.5) == Parts(value: 0, unit: .second))
    }

    @Test("±59.5 seconds: the past stays in seconds, the future becomes a minute")
    func fiftyNineAndAHalf() {
        #expect(Self.format(-59.5) == "59 seconds ago")
        #expect(Self.format(59.5) == "in 1 minute")
    }

    @Test("27 to 34 days never read 0 months")
    func fourToFiveWeeks() {
        #expect(Self.format(-27 * Self.day) == "4 weeks ago")
        #expect(Self.format(-28 * Self.day) == "4 weeks ago")
        #expect(Self.format(-29 * Self.day) == "4 weeks ago")
        #expect(Self.format(-30 * Self.day) == "4 weeks ago")
        #expect(Self.format(-34 * Self.day) == "last month")
    }

    @Test("A sweep of 1 to 800 days never yields 0 of a unit")
    func sweep() {
        for days in 1...800 {
            let past = Self.format(-Double(days) * Self.day)
            let future = Self.format(Double(days) * Self.day)
            #expect(!past.hasPrefix("0 "), "\(days) days ago: \(past)")
            #expect(!future.contains(" 0 "), "in \(days) days: \(future)")
            #expect(!past.isEmpty && !future.isEmpty, "\(days) days")
        }
    }

    @Test("Words where the locale has them, numbers otherwise")
    func words() {
        #expect(Self.format(0) == "now")
        #expect(Self.format(-Self.day) == "yesterday")
        #expect(Self.format(Self.day) == "tomorrow")
        #expect(Self.format(-2 * Self.day) == "2 days ago")
        #expect(Self.format(-7 * Self.day) == "last week")
        #expect(Self.format(7 * Self.day) == "next week")
        #expect(Self.format(-34 * Self.day) == "last month")
        #expect(Self.format(35 * Self.day) == "next month")
        #expect(Self.format(-400 * Self.day) == "last year")
        #expect(Self.format(400 * Self.day) == "next year")
        #expect(Self.format(-3 * Self.hour) == "3 hours ago")
        #expect(Self.format(-30) == "30 seconds ago")
        #expect(Self.format(-5 * Self.minute) == "5 minutes ago")
        #expect(Self.format(-Self.hour) == "1 hour ago")
        #expect(Self.format(2 * Self.day) == "in 2 days")
        #expect(Self.format(-14 * Self.day) == "2 weeks ago")
        #expect(Self.format(-60 * Self.day) == "2 months ago")
        #expect(Self.format(-1100 * Self.day) == "3 years ago")
        #expect(Self.format(-Self.day, isNumeric: true) == "1 day ago")
    }

    @Test("Other locales use their own words, including the two-day words")
    func otherLocales() {
        let de = Locale(identifier: "de_DE")
        let now = Self.now
        #expect(MonarchRelativeTime.string(Self.at(-3 * Self.hour), now: now, locale: de) == "vor 3 Stunden")
        #expect(MonarchRelativeTime.string(Self.at(-2 * Self.day), now: now, locale: de) == "vorgestern")
        #expect(
            MonarchRelativeTime.string(Self.at(-2 * Self.day), now: now, locale: de, isNumeric: true) == "vor 2 Tagen"
        )
        let fr = Locale(identifier: "fr_FR")
        #expect(MonarchRelativeTime.string(Self.at(-5 * Self.minute), now: now, locale: fr) == "il y a 5 minutes")
    }

    @Test("The future reads as now unless it is allowed; the short style is shorter")
    func futureAndStyle() {
        let ahead = Self.at(2 * Self.hour)
        #expect(MonarchRelativeTime.string(ahead, now: Self.now, locale: Self.en) == "now")
        #expect(MonarchRelativeTime.string(ahead, now: Self.now, locale: Self.en, allowsFuture: true) == "in 2 hours")
        let short = MonarchRelativeTime.string(
            Self.at(-3 * Self.minute),
            now: Self.now,
            locale: Self.en,
            unitsStyle: .short
        )
        #expect(short == "3 min. ago")
    }

    @Test("Never switches to a date by itself; a component's absoluteAfter is strict")
    func neverAbsolute() {
        #expect(Self.format(-3653 * Self.day) == "10 years ago")
        #expect(!MonarchRelativeTime.showsAbsolute(Self.at(-3653 * Self.day), now: Self.now, absoluteAfter: nil))
        let week = 7 * Self.day
        #expect(!MonarchRelativeTime.showsAbsolute(Self.at(-week), now: Self.now, absoluteAfter: week))
        #expect(MonarchRelativeTime.showsAbsolute(Self.at(-week - 1), now: Self.now, absoluteAfter: week))
        #expect(MonarchRelativeTime.showsAbsolute(Self.at(8 * Self.day), now: Self.now, absoluteAfter: week))
    }

    @Test("The redraw follows the web live interval")
    func refresh() {
        #expect(MonarchRelativeTime.refreshInterval(for: Self.at(-10), now: Self.now) == 5)
        #expect(MonarchRelativeTime.refreshInterval(for: Self.at(-5 * Self.minute), now: Self.now) == 30)
        #expect(MonarchRelativeTime.refreshInterval(for: Self.at(-5 * Self.hour), now: Self.now) == 300)
        #expect(MonarchRelativeTime.refreshInterval(for: Self.at(-3 * Self.day), now: Self.now) == 3600)
        let dates: [Date?] = [nil, Self.at(-3 * Self.day), Self.at(-2)]
        #expect(MonarchRelativeTime.refreshInterval(for: dates, now: Self.now) == 5)
        #expect(MonarchRelativeTime.refreshInterval(for: [], now: Self.now) == 3600)
    }

    @Test("The schedule slows down as a time gets older")
    func schedule() {
        let start = Self.at(50)
        var entries = MonarchRelativeTimeSchedule(dates: [Self.now]).entries(from: start, mode: .normal)
        let first = entries.next()
        let second = entries.next()
        let third = entries.next()
        #expect(first == start)
        #expect(second == start.addingTimeInterval(5))
        // 55 s old: still in the first minute.
        #expect(third == start.addingTimeInterval(10))
        // 60 s old: every 30 seconds now.
        #expect(entries.next() == start.addingTimeInterval(40))
        var low = MonarchRelativeTimeSchedule(dates: [Self.now]).entries(from: start, mode: .lowFrequency)
        _ = low.next()
        #expect(low.next() == start.addingTimeInterval(60))
    }
}
