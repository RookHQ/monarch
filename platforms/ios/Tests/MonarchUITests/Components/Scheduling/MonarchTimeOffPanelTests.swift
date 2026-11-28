import Foundation
import SwiftUI
import Testing

@_spi(Experimental) @testable import MonarchUI

/// A Gregorian calendar in UTC, so days do not depend on the machine.
private let timeOffCalendar: Calendar = {
    var calendar = Calendar(identifier: .gregorian)
    calendar.timeZone = TimeZone(identifier: "UTC") ?? .gmt
    calendar.locale = Locale(identifier: "en_US")
    return calendar
}()

private let timeOffLocale = Locale(identifier: "en_US")

private func timeOffDate(_ month: Int, _ day: Int, _ hour: Int = 0, _ minute: Int = 0) -> Date {
    timeOffCalendar.date(from: DateComponents(year: 2026, month: month, day: day, hour: hour, minute: minute))
        ?? Date(timeIntervalSince1970: 0)
}

/// Wednesday 11 November 2026, 10:00 UTC.
private let timeOffNow = timeOffDate(11, 11, 10)

private let timeOffEntries = [
    MonarchTimeOffEntry(
        id: "leave",
        personID: "ana",
        personName: "Ana Lima",
        isAllDay: true,
        start: timeOffDate(11, 9),
        end: timeOffDate(11, 13),
        type: "Annual leave",
        reason: "Family visit",
        status: "Pending"
    ),
    MonarchTimeOffEntry(
        id: "closure",
        personID: nil,
        isAllDay: true,
        start: timeOffDate(12, 25),
        end: timeOffDate(12, 25)
    ),
    MonarchTimeOffEntry(
        id: "training",
        personID: "kai",
        personName: "Kai Chen",
        isAllDay: false,
        start: timeOffDate(11, 11, 9),
        end: timeOffDate(11, 11, 9, 30)
    ),
    MonarchTimeOffEntry(
        id: "old",
        personID: "kai",
        personName: "Kai Chen",
        isAllDay: true,
        start: timeOffDate(10, 1),
        end: timeOffDate(10, 2),
        type: "Sick leave"
    ),
]

private let timeOffPeople = [
    MonarchTimeOffOption("Ana Lima", value: "ana"), MonarchTimeOffOption("Kai Chen", value: "kai"),
]
private let timeOffTypes = [
    MonarchTimeOffOption("Annual leave", value: "annual"), MonarchTimeOffOption("Sick leave", value: "sick"),
]

private struct TimeOffTestError: LocalizedError {
    var errorDescription: String? { "The server is busy." }
}

private struct SilentTimeOffError: Error {}

enum MonarchTimeOffPanelFixtures: ComponentFixtures {
    static let id = "time-off-panel"
    static let fixtures = [
        Fixture("entries, add and remove, notice") {
            MonarchTimeOffPanel(
                entries: timeOffEntries,
                timeZone: timeOffCalendar.timeZone,
                now: timeOffNow,
                people: timeOffPeople,
                allowsClosure: true,
                types: timeOffTypes,
                notice: "2 appointments fall in this period.",
                canRemove: { $0.id != "closure" },
                onAdd: { _ in },
                onRemove: { _ in }
            )
            .environment(\.calendar, timeOffCalendar)
        },
        Fixture("past included, read-only") {
            MonarchTimeOffPanel(
                entries: timeOffEntries,
                timeZone: timeOffCalendar.timeZone,
                now: timeOffNow,
                showsPast: .constant(true)
            )
            .environment(\.calendar, timeOffCalendar)
        },
        Fixture("empty") {
            MonarchTimeOffPanel(entries: [], timeZone: timeOffCalendar.timeZone, now: timeOffNow, onAdd: { _ in })
        },
        Fixture("loading") {
            MonarchTimeOffPanel(entries: [], timeZone: timeOffCalendar.timeZone, now: timeOffNow, isLoading: true)
        },
        Fixture("error with retry, custom labels") {
            MonarchTimeOffPanel(
                entries: [],
                timeZone: timeOffCalendar.timeZone,
                now: timeOffNow,
                errorMessage: "Time off could not be loaded.",
                labels: MonarchTimeOffPanelLabels(title: "Congés", retry: "Réessayer"),
                onRetry: {}
            )
        },
        Fixture("add form") {
            MonarchTimeOffAddForm(
                people: timeOffPeople,
                allowsClosure: true,
                types: timeOffTypes,
                now: timeOffNow,
                timeZone: timeOffCalendar.timeZone,
                labels: MonarchTimeOffPanelLabels(),
                onAdd: { _ in }
            )
            .environment(\.calendar, timeOffCalendar)
        },
    ]
}

@Suite("MonarchTimeOffPanel")
struct MonarchTimeOffPanelTests {
    @Test("After a removal focus goes to the row now at that place, else the one before, else the heading")
    func focusAfterRemoval() {
        typealias Tokens = MonarchTimeOffPanelTokens
        let ids = ["a", "b", "c"]
        #expect(Tokens.focusAfterRemoval(removing: "b", visibleIDs: ids) == .row("c"))
        #expect(Tokens.focusAfterRemoval(removing: "c", visibleIDs: ids) == .row("b"))
        #expect(Tokens.focusAfterRemoval(removing: "a", visibleIDs: ids) == .row("b"))
        #expect(Tokens.focusAfterRemoval(removing: "a", visibleIDs: ["a"]) == .heading)
        #expect(Tokens.focusAfterRemoval(removing: "z", visibleIDs: ids) == .heading)
    }

    private typealias Tokens = MonarchTimeOffPanelTokens
    private let labels = MonarchTimeOffPanelLabels()

    @Test("The zone is optional: nil is the device zone, a given zone wins (D3)")
    @MainActor
    func zoneParameter() {
        #expect(MonarchTimeOffPanel(entries: []).resolvedTimeZone == TimeZone.autoupdatingCurrent)
        let paris = TimeZone(identifier: "Europe/Paris")
        #expect(MonarchTimeOffPanel(entries: [], timeZone: paris).resolvedTimeZone.identifier == "Europe/Paris")
    }

    @Test("Past entries: all-day ones before today, timed ones before now")
    func past() {
        #expect(!Tokens.isPast(timeOffEntries[0], now: timeOffNow, calendar: timeOffCalendar))
        #expect(!Tokens.isPast(timeOffEntries[1], now: timeOffNow, calendar: timeOffCalendar))
        #expect(Tokens.isPast(timeOffEntries[2], now: timeOffNow, calendar: timeOffCalendar))
        #expect(Tokens.isPast(timeOffEntries[3], now: timeOffNow, calendar: timeOffCalendar))
        // An all-day entry that ends today is not past at the end of the day.
        let today = MonarchTimeOffEntry(
            id: "t",
            personID: "a",
            isAllDay: true,
            start: timeOffDate(11, 11),
            end: timeOffDate(11, 11)
        )
        #expect(!Tokens.isPast(today, now: timeOffDate(11, 11, 23, 59), calendar: timeOffCalendar))
        let ids = { (showsPast: Bool) in
            Tokens.visible(timeOffEntries, showsPast: showsPast, now: timeOffNow, calendar: timeOffCalendar).map(\.id)
        }
        #expect(ids(false) == ["leave", "closure"])
        #expect(ids(true) == ["leave", "closure", "training", "old"])
    }

    @Test("Who, badges and the spoken summary, so nothing relies on colour")
    func badges() {
        #expect(Tokens.who(timeOffEntries[0], labels: labels) == "Ana Lima")
        #expect(Tokens.who(timeOffEntries[1], labels: labels) == "Everyone")
        #expect(
            Tokens.who(timeOffEntries[1], labels: MonarchTimeOffPanelLabels(everyone: "Tout le monde"))
                == "Tout le monde"
        )
        let leave = Tokens.badges(timeOffEntries[0], isPast: false, labels: labels)
        #expect(leave.map(\.text) == ["Annual leave", "Pending"])
        #expect(leave.map(\.tone) == [.neutral, .info])
        let closure = Tokens.badges(timeOffEntries[1], isPast: false, labels: labels)
        #expect(closure == [MonarchTimeOffBadge(text: "Closure", tone: .warning, variant: .subtle)])
        let old = Tokens.badges(timeOffEntries[3], isPast: true, labels: labels)
        #expect(old.last == MonarchTimeOffBadge(text: "Past", tone: .neutral, variant: .outline))
        #expect(
            Tokens.summary(who: "Ana Lima", badges: leave, detail: "Mon (all day) · Family visit")
                == "Ana Lima, Annual leave, Pending, Mon (all day) · Family visit"
        )
        #expect(Tokens.detail("Mon", reason: nil) == "Mon")
        #expect(Tokens.detail("Mon", reason: "") == "Mon")
        #expect(Tokens.detail("Mon", reason: "Conference") == "Mon · Conference")
    }

    @Test("Dates are written in the locale and the panel's time zone")
    func when() {
        #expect(
            Tokens.when(timeOffEntries[0], labels: labels, locale: timeOffLocale, calendar: timeOffCalendar)
                == "Mon, Nov 9, 2026 – Fri, Nov 13, 2026 (all day)"
        )
        #expect(
            Tokens.when(timeOffEntries[1], labels: labels, locale: timeOffLocale, calendar: timeOffCalendar)
                == "Fri, Dec 25, 2026 (all day)"
        )
        let timed = Tokens.when(timeOffEntries[2], labels: labels, locale: timeOffLocale, calendar: timeOffCalendar)
        #expect(timed.hasPrefix("Wed, Nov 11, 2026, 9:00"))
        #expect(timed.contains("9:30"))
        let overnight = MonarchTimeOffEntry(
            id: "n",
            personID: "a",
            isAllDay: false,
            start: timeOffDate(11, 11, 22),
            end: timeOffDate(11, 12, 6)
        )
        let text = Tokens.when(overnight, labels: labels, locale: timeOffLocale, calendar: timeOffCalendar)
        #expect(text.hasPrefix("Wed, Nov 11, 2026, 10:00"))
        #expect(text.contains("Thu, Nov 12, 2026, 6:00"))
        // The same moment is another day in Tokyo.
        var tokyo = timeOffCalendar
        tokyo.timeZone = TimeZone(identifier: "Asia/Tokyo") ?? .gmt
        #expect(
            Tokens.when(overnight, labels: labels, locale: timeOffLocale, calendar: tokyo).hasPrefix(
                "Thu, Nov 12, 2026, 7:00"
            )
        )
        let custom = MonarchTimeOffPanelLabels(allDay: "toute la journée")
        #expect(
            Tokens.when(timeOffEntries[1], labels: custom, locale: timeOffLocale, calendar: timeOffCalendar)
                .hasSuffix("(toute la journée)")
        )
    }

    @Test("Remove is named by who and when, and only offered where allowed")
    func remove() {
        #expect(
            Tokens.removeLabel(who: "Ana Lima", when: "Mon", labels: labels) == "Remove time off for Ana Lima, Mon"
        )
        #expect(
            Tokens.removeLabel(
                who: "Ana",
                when: "Mon",
                labels: MonarchTimeOffPanelLabels(remove: { "Supprimer \($0) \($1)" })
            )
                == "Supprimer Ana Mon"
        )
        #expect(
            Tokens.removeMessage(who: "Ana Lima", when: "Mon", labels: labels)
                == "Ana Lima · Mon. Those times become available again if they fall within working hours."
        )
        #expect(Tokens.isRemovable(timeOffEntries[0], hasRemove: true, canRemove: nil))
        #expect(!Tokens.isRemovable(timeOffEntries[0], hasRemove: false, canRemove: nil))
        #expect(!Tokens.isRemovable(timeOffEntries[1], hasRemove: true, canRemove: { $0.personID != nil }))
        #expect(Tokens.failure(TimeOffTestError(), labels: labels) == "The server is busy.")
        #expect(Tokens.failure(SilentTimeOffError(), labels: labels) == "That did not work. Try again.")
    }

    @Test("The form opens on the first person and type, whole days from today, 9 to 5")
    func initialForm() {
        let form = Tokens.initialForm(
            people: timeOffPeople,
            types: timeOffTypes,
            now: timeOffNow,
            calendar: timeOffCalendar
        )
        #expect(form.personID == "ana")
        #expect(form.type == "annual")
        #expect(form.isAllDay)
        #expect(form.fromDay == timeOffDate(11, 11))
        #expect(form.untilDay == timeOffDate(11, 11))
        #expect(form.start == timeOffDate(11, 11, 9))
        #expect(form.end == timeOffDate(11, 11, 17))
        let closureOnly = Tokens.initialForm(people: [], types: [], now: timeOffNow, calendar: timeOffCalendar)
        #expect(closureOnly.personID == nil && closureOnly.type == nil)
        #expect(
            Tokens.whoOptions(people: timeOffPeople, allowsClosure: true) == [timeOffPeople[0], timeOffPeople[1], nil]
        )
        #expect(Tokens.whoOptions(people: timeOffPeople, allowsClosure: false).count == 2)
    }

    @Test("The last day follows the first; an end before the start is refused")
    func validation() {
        var form = Tokens.initialForm(people: timeOffPeople, types: [], now: timeOffNow, calendar: timeOffCalendar)
        form = Tokens.settingFromDay(timeOffDate(11, 20, 15), form: form, calendar: timeOffCalendar)
        #expect(form.fromDay == timeOffDate(11, 20))
        #expect(form.untilDay == timeOffDate(11, 20))
        #expect(Tokens.problem(form, labels: labels, calendar: timeOffCalendar) == nil)
        form.untilDay = timeOffDate(11, 19)
        #expect(Tokens.problem(form, labels: labels, calendar: timeOffCalendar) == "The end must be after the start.")
        form.isAllDay = false
        #expect(Tokens.problem(form, labels: labels, calendar: timeOffCalendar) == nil)
        form.end = form.start
        #expect(Tokens.problem(form, labels: labels, calendar: timeOffCalendar) == "The end must be after the start.")
        #expect(
            Tokens.problem(
                form,
                labels: MonarchTimeOffPanelLabels(endBeforeStart: "Fin avant début"),
                calendar: timeOffCalendar
            )
                == "Fin avant début"
        )
    }

    @Test("The draft has whole days, the type when offered and a trimmed reason")
    func draft() {
        var form = Tokens.initialForm(
            people: timeOffPeople,
            types: timeOffTypes,
            now: timeOffNow,
            calendar: timeOffCalendar
        )
        form.untilDay = timeOffDate(11, 14, 12)
        form.reason = "  Conference \n"
        let draft = Tokens.draft(form, offersTypes: true, calendar: timeOffCalendar)
        #expect(
            draft
                == MonarchTimeOffDraft(
                    personID: "ana",
                    isAllDay: true,
                    start: timeOffDate(11, 11),
                    end: timeOffDate(11, 14),
                    type: "annual",
                    reason: "Conference"
                )
        )
        form.isAllDay = false
        form.personID = nil
        form.reason = "   "
        let timed = Tokens.draft(form, offersTypes: false, calendar: timeOffCalendar)
        #expect(
            timed
                == MonarchTimeOffDraft(
                    personID: nil,
                    isAllDay: false,
                    start: timeOffDate(11, 11, 9),
                    end: timeOffDate(11, 11, 17)
                )
        )
        #expect(Tokens.limited(String(repeating: "a", count: 250)).count == 200)
        #expect(Tokens.limited("Short") == "Short")
        let note = Tokens.zoneNote(TimeZone(identifier: "Europe/Paris") ?? .gmt, labels: labels, locale: timeOffLocale)
        #expect(note.hasPrefix("Dates and times are in this time zone: "))
        #expect(note.count > "Dates and times are in this time zone: ".count)
    }

    @Test("The built-in strings resolve")
    func strings() {
        #expect(MonarchTimeOffPanelStrings.title == "Time off")
        #expect(MonarchTimeOffPanelStrings.add == "Add time off")
        #expect(MonarchTimeOffPanelStrings.includePast == "Include past")
        #expect(MonarchTimeOffPanelStrings.closure == "Closure")
        #expect(MonarchTimeOffPanelStrings.everyone == "Everyone")
        #expect(MonarchTimeOffPanelStrings.past == "Past")
        #expect(MonarchTimeOffPanelStrings.allDay == "all day")
        #expect(MonarchTimeOffPanelStrings.allDayToggle == "All day")
        #expect(MonarchTimeOffPanelStrings.removeTitle == "Remove this time off?")
        #expect(MonarchTimeOffPanelStrings.removeConfirm == "Remove")
        #expect(MonarchTimeOffPanelStrings.emptyTitle == "No time off planned")
        #expect(MonarchTimeOffPanelStrings.retry == "Try again")
        #expect(MonarchTimeOffPanelStrings.loading == "Loading time off")
        #expect(MonarchTimeOffPanelStrings.formTitle == "Add time off")
        #expect(MonarchTimeOffPanelStrings.who == "Who")
        #expect(MonarchTimeOffPanelStrings.type == "Type")
        #expect(MonarchTimeOffPanelStrings.closureNotice == "A closure blocks bookings for everyone.")
        #expect(MonarchTimeOffPanelStrings.from == "From")
        #expect(MonarchTimeOffPanelStrings.until == "Until (inclusive)")
        #expect(MonarchTimeOffPanelStrings.start == "Start")
        #expect(MonarchTimeOffPanelStrings.end == "End")
        #expect(MonarchTimeOffPanelStrings.reason == "Reason (optional)")
        #expect(MonarchTimeOffPanelStrings.reasonHelper == "Visible to your team.")
        #expect(MonarchTimeOffPanelStrings.cancel == "Cancel")
        #expect(MonarchTimeOffPanelStrings.save == "Add")
        #expect(MonarchTimeOffPanelStrings.failed == "That did not work. Try again.")
        #expect(
            MonarchTimeOffPanelStrings.message
                == "Holidays, sickness, training or closures. Nobody is booked in these periods."
        )
        #expect(
            MonarchTimeOffPanelStrings.emptyMessage
                == "Add holidays or closures so nobody is offered times when no one is available."
        )
    }
}
