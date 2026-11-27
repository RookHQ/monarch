import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

private typealias Tokens = MonarchBusinessHoursEditorTokens
private typealias Value = MonarchBusinessHoursValue
private typealias Exception = MonarchBusinessHoursException
private typealias Interval = MonarchBusinessHoursInterval

private enum BusinessHoursSamples {
    /// 2026-12-25 and 2026-12-31 at midnight UTC.
    static let christmas = Date(timeIntervalSince1970: 1_798_156_800)
    static let newYearsEve = Date(timeIntervalSince1970: 1_798_675_200)
    static let week = (1...5).map { MonarchWeeklyHoursInterval(day: $0, start: 540, end: 1020) }

    static let support = Value(
        timeZone: "Europe/London",
        hours: week,
        exceptions: [
            // A duplicate too: the unnamed row above already has this day, as on the web.
            Exception(date: christmas, name: "Christmas Day"),
            Exception(date: newYearsEve, name: "New Year’s Eve", intervals: [Interval(start: 540, end: 780)]),
        ],
        name: "Support hours"
    )

    static let invalid = Value(
        timeZone: "Asia/Kolkata",
        hours: [MonarchWeeklyHoursInterval(day: 2, start: 600, end: 540)],
        exceptions: [
            Exception(date: nil, name: "No date yet"),
            Exception(date: christmas, name: "  "),
            Exception(date: christmas, name: "Christmas Day"),
            // The same day at another hour is still the same day.
            Exception(date: christmas.addingTimeInterval(3_600), name: "Christmas again"),
            Exception(date: newYearsEve, name: "Short day", intervals: [Interval(start: 900, end: 600)]),
        ],
        effectiveFrom: newYearsEve,
        effectiveTo: christmas
    )
}

enum MonarchBusinessHoursEditorFixtures: ComponentFixtures {
    static let id = "business-hours-editor"
    static let fixtures = [
        Fixture("support hours with a holiday and special hours") {
            Form {
                MonarchBusinessHoursEditor(value: .constant(BusinessHoursSamples.support), firstWeekday: 2)
            }
            .frame(height: 1_600)
        },
        Fixture("every part, every problem shown, 24-hour clock") {
            Form {
                MonarchBusinessHoursEditor(
                    value: .constant(BusinessHoursSamples.invalid),
                    showsDefault: true,
                    showsEffectiveDates: true,
                    isNameRequired: true,
                    isOpenRequired: true,
                    showsErrors: true,
                    pinnedTimeZones: ["Europe/London"],
                    firstWeekday: 1,
                    uses24HourClock: true,
                    step: 30
                ) {
                    Section("Escalation") { Text(verbatim: "Page the on-call engineer after 30 minutes") }
                }
            }
            .frame(height: 2_000)
        },
        Fixture("empty, closed dates only, no name") {
            Form {
                MonarchBusinessHoursEditor(
                    value: .constant(Value(timeZone: "UTC")),
                    showsName: false,
                    allowsSpecialHours: false,
                    isOpenRequired: true
                )
            }
            .frame(height: 1_200)
        },
        Fixture("read-only with effective dates and default") {
            Form {
                MonarchBusinessHoursEditor(
                    value: .constant(
                        Value(
                            timeZone: "America/New_York",
                            hours: BusinessHoursSamples.week,
                            exceptions: BusinessHoursSamples.support.exceptions + [Exception(date: nil, name: "")],
                            name: "",
                            isDefault: true,
                            effectiveFrom: BusinessHoursSamples.christmas
                        )
                    ),
                    showsDefault: true,
                    showsEffectiveDates: true,
                    uses24HourClock: false,
                    isReadOnly: true
                )
            }
            .frame(height: 1_600)
        },
        Fixture("disabled, long labels") {
            Form {
                MonarchBusinessHoursEditor(
                    value: .constant(BusinessHoursSamples.support),
                    showsEffectiveDates: true,
                    labels: MonarchBusinessHoursEditorLabels(
                        exceptions: "Public holidays, company closures and days with shorter opening hours",
                        exceptionsDescription:
                            "Each date replaces the weekly hours for the whole day in the calendar's time zone"
                    )
                )
                .disabled(true)
            }
            .frame(height: 1_600)
        },
    ]
}

@Suite("MonarchBusinessHoursEditor")
@MainActor
struct MonarchBusinessHoursEditorTests {
    @Test("After a date is removed focus goes to the date now at that place, else the one before, else Add date")
    func focusAfterRemoval() {
        typealias Tokens = MonarchBusinessHoursEditorTokens
        #expect(Tokens.focusAfterRemoval(removedIndex: 1, count: 3) == .date(1))
        #expect(Tokens.focusAfterRemoval(removedIndex: 2, count: 3) == .date(1))
        #expect(Tokens.focusAfterRemoval(removedIndex: 0, count: 2) == .date(0))
        #expect(Tokens.focusAfterRemoval(removedIndex: 0, count: 1) == .add)
    }

    private let text = MonarchBusinessHoursEditorText(labels: MonarchBusinessHoursEditorLabels())
    private let weeklyText = MonarchWeeklyHoursEditorText(labels: MonarchWeeklyHoursEditorLabels())
    private let us = Locale(identifier: "en_US")
    private let gb = Locale(identifier: "en_GB")

    @Test("A complete value is valid")
    func valid() {
        let errors = BusinessHoursSamples.support.validate(isNameRequired: true, isOpenRequired: true)
        #expect(errors.isValid)
        #expect(errors == MonarchBusinessHoursErrors())
        #expect(MonarchBusinessHoursErrors().isValid)
    }

    @Test("Validation finds each row's first problem, duplicates by UTC day and the effective order")
    func invalid() {
        let errors = BusinessHoursSamples.invalid.validate()
        #expect(!errors.isValid)
        #expect(errors.exceptions == [0: .date, 1: .name, 2: .duplicate, 3: .duplicate, 4: .hours])
        #expect(errors.hours == [2: .order])
        #expect(errors.isEffectiveOrderInvalid)
        // Name and open hours are only checked when asked for.
        #expect(!errors.isNameMissing)
        #expect(!errors.isOpenIntervalMissing)
        let strict = BusinessHoursSamples.invalid.validate(isNameRequired: true, isOpenRequired: true)
        #expect(strict.isNameMissing)
        #expect(strict.isOpenIntervalMissing)
    }

    @Test("Special hours must fall within the day and the effective days may be equal")
    func edges() {
        let outside = Value(
            timeZone: "UTC",
            exceptions: [
                Exception(
                    date: BusinessHoursSamples.christmas,
                    name: "Late",
                    intervals: [Interval(start: 1_320, end: 1_500)]
                )
            ]
        )
        #expect(outside.validate().exceptions == [0: .hours])
        let midnight = Value(
            timeZone: "UTC",
            exceptions: [
                Exception(
                    date: BusinessHoursSamples.christmas,
                    name: "Late",
                    intervals: [Interval(start: 1_320, end: 1_440)]
                )
            ]
        )
        #expect(midnight.validate().isValid)
        let sameDay = Value(
            timeZone: "UTC",
            effectiveFrom: BusinessHoursSamples.christmas.addingTimeInterval(7_200),
            effectiveTo: BusinessHoursSamples.christmas
        )
        #expect(!sameDay.validate().isEffectiveOrderInvalid)
        let blank = Value(timeZone: "UTC", name: " \n")
        #expect(blank.validate(isNameRequired: true).isNameMissing)
        let closedWeek = Value(timeZone: "UTC", hours: [MonarchWeeklyHoursInterval(day: 1, start: 600, end: 600)])
        #expect(closedWeek.validate(isOpenRequired: true).isOpenIntervalMissing)
    }

    @Test("Days are UTC days: today follows the person's zone and the minimum day")
    func days() throws {
        let kolkata = try #require(TimeZone(identifier: "Asia/Kolkata"))
        let losAngeles = try #require(TimeZone(identifier: "America/Los_Angeles"))
        // 2026-12-24 at 20:00 UTC is already the 25th in Kolkata and still the 24th in Los Angeles.
        let now = BusinessHoursSamples.christmas.addingTimeInterval(-4 * 3_600)
        #expect(Tokens.today(now: now, in: kolkata) == BusinessHoursSamples.christmas)
        #expect(Tokens.today(now: now, in: losAngeles) == BusinessHoursSamples.christmas.addingTimeInterval(-86_400))
        #expect(Tokens.day(BusinessHoursSamples.christmas.addingTimeInterval(80_000)) == BusinessHoursSamples.christmas)
        let today = BusinessHoursSamples.christmas
        #expect(Tokens.firstChoice(today: today, minimum: nil) == today)
        #expect(
            Tokens.firstChoice(today: today, minimum: BusinessHoursSamples.newYearsEve)
                == BusinessHoursSamples.newYearsEve
        )
        #expect(
            Tokens.firstChoice(today: BusinessHoursSamples.newYearsEve, minimum: today)
                == BusinessHoursSamples.newYearsEve
        )
        let pickerZone = Tokens.pickerCalendar(Calendar(identifier: .gregorian)).timeZone
        #expect(pickerZone.secondsFromGMT(for: BusinessHoursSamples.christmas) == 0)
    }

    @Test("Dates read as the UTC day in the locale")
    func dateText() {
        let gregorian = Calendar(identifier: .gregorian)
        #expect(Tokens.dateText(BusinessHoursSamples.christmas, locale: gb, calendar: gregorian) == "25 Dec 2026")
        #expect(Tokens.dateText(BusinessHoursSamples.christmas, locale: us, calendar: gregorian) == "Dec 25, 2026")
    }

    @Test("Switching a row between closed and open keeps or sets its hours")
    func kind() {
        let closed = Exception(date: BusinessHoursSamples.christmas, name: "Christmas Day")
        let opened = Tokens.settingOpen(closed, isOpen: true)
        #expect(opened.intervals == [Interval(start: 600, end: 960)])
        let custom = Exception(
            date: nil,
            name: "",
            intervals: [Interval(start: 540, end: 780), Interval(start: 800, end: 900)]
        )
        #expect(Tokens.settingOpen(custom, isOpen: true) == custom)
        #expect(Tokens.settingOpen(custom, isOpen: false).intervals.isEmpty)
        // Editing the first interval keeps the rest.
        let edited = Tokens.settingFirst(custom, start: 600)
        #expect(edited.intervals == [Interval(start: 600, end: 780), Interval(start: 800, end: 900)])
        #expect(Tokens.settingFirst(custom, end: 840).intervals.first == Interval(start: 540, end: 840))
        #expect(Tokens.settingFirst(closed, start: 600) == closed)
    }

    @Test("Rows are replaced and removed by index, and their changed state follows them")
    func rows() {
        let rows = BusinessHoursSamples.invalid.exceptions
        let named = Exception(date: nil, name: "Named")
        #expect(Tokens.replacing(rows, at: 0, with: named)[0] == named)
        #expect(Tokens.replacing(rows, at: 9, with: named) == rows)
        #expect(
            Tokens.removing(rows, at: 1).map(\.name) == [
                "No date yet", "Christmas Day", "Christmas again", "Short day",
            ]
        )
        let touched: Set<MonarchBusinessHoursEditorField> = [.name, .exception(0), .exception(1), .exception(3)]
        #expect(Tokens.touched(touched, removing: 1) == [.name, .exception(0), .exception(2)])
        #expect(Tokens.limited(String(repeating: "a", count: 140)).count == 100)
        #expect(Tokens.limited("Support") == "Support")
    }

    @Test("Problems show once changed or when asked for; the effective order always shows")
    func problems() {
        let errors = BusinessHoursSamples.invalid.validate(isNameRequired: true, isOpenRequired: true)
        let quiet = Tokens.problems(
            errors,
            touched: [],
            showsErrors: false,
            showsName: true,
            showsEffectiveDates: true,
            text: text
        )
        #expect(quiet == MonarchBusinessHoursEditorProblems(effective: text.effectiveOrder))
        let some = Tokens.problems(
            errors,
            touched: [.name, .exception(3)],
            showsErrors: false,
            showsName: true,
            showsEffectiveDates: false,
            text: text
        )
        #expect(some.name == "Enter a name.")
        #expect(some.effective == nil)
        #expect(some.open == nil)
        #expect(some.exceptions == [3: "This date is listed twice."])
        let all = Tokens.problems(
            errors,
            touched: [],
            showsErrors: true,
            showsName: false,
            showsEffectiveDates: true,
            text: text
        )
        #expect(all.name == nil)
        #expect(all.open == "Open at least one interval.")
        #expect(
            all.all == [
                "The end date must be on or after the start date.", "Open at least one interval.", "Choose a date.",
                "Give the date a name.", "This date is listed twice.", "This date is listed twice.",
                "The closing time must be after the opening time.",
            ]
        )
        #expect(Tokens.newMessages(from: ["A", "B"], to: ["B", "C"]) == ["C"])
        #expect(Tokens.newMessages(from: ["A"], to: []).isEmpty)
    }

    @Test("Read-only hours read as closed or as the intervals")
    func hoursText() {
        let formats = MonarchWeeklyHoursEditorFormats(locale: us, uses24HourClock: false, midnight: text.endOfDay)
        let closed = BusinessHoursSamples.support.exceptions[0]
        let open = BusinessHoursSamples.support.exceptions[1]
        #expect(Tokens.hoursText(closed, text: text, weeklyText: weeklyText, formats: formats) == "Closed all day")
        let short = Tokens.hoursText(open, text: text, weeklyText: weeklyText, formats: formats)
        #expect(short.contains("9:00"))
        #expect(short.contains("1:00"))
        let late = Exception(date: nil, name: "", intervals: [Interval(start: 1_200, end: 1_440)])
        #expect(Tokens.hoursText(late, text: text, weeklyText: weeklyText, formats: formats).contains("End of day"))
        let formats24 = MonarchWeeklyHoursEditorFormats(locale: gb, uses24HourClock: true, midnight: text.endOfDay)
        #expect(Tokens.hoursText(late, text: text, weeklyText: weeklyText, formats: formats24).contains("24:00"))
    }

    @Test("Spoken names carry the row's position or name")
    func accessibility() {
        #expect(text.exceptionDate(2) == "Date 2")
        #expect(text.exceptionName(2) == "Name of date 2")
        #expect(text.exceptionKind(2) == "Hours on date 2")
        #expect(text.exceptionStart(2) == "Date 2 opens")
        #expect(text.exceptionEnd(2) == "Date 2 closes")
        #expect(text.removeException(1, name: " Christmas Day ") == "Remove Christmas Day")
        #expect(text.removeException(3, name: "  ") == "Remove date 3")
        #expect(text.clearDateItem("Effective from") == "Clear Effective from")
        #expect(text.rowTitle(Exception(date: nil, name: "Boxing Day"), position: 1) == "Boxing Day")
        #expect(text.rowTitle(Exception(date: nil, name: " "), position: 4) == "Date 4")
    }

    @Test("The built-in strings resolve")
    func strings() {
        for key in MonarchBusinessHoursEditorStrings.keys {
            let value = MonarchBusinessHoursEditorStrings.string(key)
            #expect(!value.hasPrefix("monarch."), "\(key) does not resolve")
        }
        #expect(text.name == "Name")
        #expect(text.timeZone == "Time zone")
        #expect(text.exceptions == "Holidays and exceptions")
        #expect(text.addException == "Add date")
        #expect(text.closed == "Closed all day")
        #expect(text.specialHours == "Open at other times")
        #expect(text.chooseDate == "Choose a date")
        #expect(text.noDate == "Not set")
        #expect(text.endOfDay == "End of day")
        #expect(text.defaultTitle == "Default")
        for error in MonarchBusinessHoursExceptionError.allCases {
            #expect(!text.exceptionError(error).hasPrefix("monarch."))
        }
    }

    @Test("Replacements win over the built-in strings")
    func overrides() {
        let custom = MonarchBusinessHoursEditorText(
            labels: MonarchBusinessHoursEditorLabels(
                name: "Nom",
                exceptionDate: { "Jour \($0)" },
                removeException: { position, name in "Supprimer \(position) \(name)" },
                exceptionError: { "E:\($0.rawValue)" },
                clearDateItem: { "Effacer \($0)" }
            )
        )
        #expect(custom.name == "Nom")
        #expect(custom.exceptionDate(2) == "Jour 2")
        #expect(custom.removeException(2, name: " Noël ") == "Supprimer 2 Noël")
        #expect(custom.exceptionError(.duplicate) == "E:duplicate")
        #expect(custom.clearDateItem("Du") == "Effacer Du")
        #expect(custom.rowTitle(Exception(date: nil, name: ""), position: 3) == "Jour 3")
        #expect(custom.closed == "Closed all day")
    }

    #if os(iOS)
        @Test("The editor lays out at the largest text size")
        func largestText() {
            let size = Host.layout(
                Form { MonarchBusinessHoursEditor(value: .constant(BusinessHoursSamples.support)) }
                    .frame(height: 1_600)
                    .environment(\.dynamicTypeSize, .accessibility5)
            )
            #expect(size.height > 0)
        }
    #endif
}
