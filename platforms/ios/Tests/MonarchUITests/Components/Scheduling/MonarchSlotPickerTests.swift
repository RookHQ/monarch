import Foundation
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchSlotPickerFixtures: ComponentFixtures {
    static let id = "slot-picker"

    /// 4 November 2026, 00:00 UTC.
    static let day = Date(timeIntervalSince1970: 1_793_750_400)

    static var calendar: Calendar {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(identifier: "UTC") ?? .gmt
        return calendar
    }

    static func at(_ hours: Double) -> Date { day.addingTimeInterval(hours * 3_600) }

    static let slots: [MonarchSlotPickerSlot] = [
        MonarchSlotPickerSlot(start: at(9)),
        MonarchSlotPickerSlot(start: at(9.5), isDisabled: true),
        MonarchSlotPickerSlot(start: at(10)),
        MonarchSlotPickerSlot(start: at(13.5), seatsLeft: 2),
        MonarchSlotPickerSlot(start: at(15)),
    ]

    @MainActor
    static func picker(
        selection: Date? = nil,
        day: Date? = Self.day,
        slots: [MonarchSlotPickerSlot] = Self.slots,
        isLoading: Bool = false,
        errorMessage: String? = nil,
        businessTimeZone: String? = nil,
        zonePicker: Bool = false
    ) -> some View {
        var onZone: (@MainActor (String) -> Void)?
        if zonePicker { onZone = { _ in } }
        return MonarchSlotPicker(
            selection: .constant(selection),
            day: .constant(day),
            slots: slots,
            timeZone: "UTC",
            minimumDate: Self.day,
            businessTimeZone: businessTimeZone,
            isLoading: isLoading,
            errorMessage: errorMessage,
            onTimeZoneChange: onZone,
            onRetry: {},
            onChooseOutside: {}
        )
        .environment(\.calendar, calendar)
        .environment(\.locale, Locale(identifier: "en_US"))
    }

    static let fixtures: [Fixture] = [
        Fixture("times with a selection and seats") { picker(selection: at(10)) },
        Fixture("no day yet") { picker(day: nil) },
        Fixture("loading") { picker(isLoading: true) },
        Fixture("error with retry") { picker(errorMessage: "The calendar did not answer.") },
        Fixture("empty day with a way out") { picker(slots: []) },
        Fixture("selection is not an open time") { picker(selection: at(11)) },
        Fixture("zone picker and converted note") { picker(businessTimeZone: "America/Los_Angeles", zonePicker: true) },
    ]
}

@Suite("MonarchSlotPicker")
struct MonarchSlotPickerTests {
    private typealias Tokens = MonarchSlotPickerTokens
    private typealias F = MonarchSlotPickerFixtures
    private let locale = Locale(identifier: "en_US")
    private let none = MonarchSlotPickerLabels()

    @Test("Days outside availableDays or disabled by the app cannot be chosen")
    func days() {
        let calendar = F.calendar
        let next = F.day.addingTimeInterval(86_400)
        #expect(!Tokens.isDayDisabled(F.day, availableDays: nil, calendar: calendar, custom: false))
        #expect(Tokens.isDayDisabled(F.day, availableDays: nil, calendar: calendar, custom: true))
        #expect(!Tokens.isDayDisabled(F.day, availableDays: [F.at(12)], calendar: calendar, custom: false))
        #expect(Tokens.isDayDisabled(next, availableDays: [F.at(12)], calendar: calendar, custom: false))
    }

    @Test("Days and times are formatted with the given calendar, locale and zone")
    func formatting() {
        #expect(Tokens.dayTitle(F.day, calendar: F.calendar, locale: locale) == "Wednesday, November 4")
        let utc = TimeZone(identifier: "UTC") ?? .gmt
        let paris = TimeZone(identifier: "Europe/Paris") ?? .gmt
        let time = Tokens.timeText(F.at(9.5), timeZone: utc, calendar: F.calendar, locale: locale)
        #expect(time.hasPrefix("9:30"))
        #expect(time.hasSuffix("AM"))
        #expect(Tokens.timeText(F.at(9.5), timeZone: paris, calendar: F.calendar, locale: locale).hasPrefix("10:30"))
    }

    @Test("Pressing a time selects it; again keeps or clears it; disabled times do nothing")
    func selection() {
        let open = F.slots[0]
        let taken = F.slots[1]
        #expect(Tokens.selection(afterPressing: open, current: nil, allowsDeselect: false) == open.start)
        #expect(Tokens.selection(afterPressing: open, current: open.start, allowsDeselect: false) == open.start)
        #expect(Tokens.selection(afterPressing: open, current: open.start, allowsDeselect: true) == nil)
        #expect(Tokens.selection(afterPressing: taken, current: open.start, allowsDeselect: true) == open.start)
        #expect(Tokens.selectedIndex(F.at(10), slots: F.slots) == 2)
        #expect(Tokens.selectedIndex(nil, slots: F.slots) == nil)
        #expect(Tokens.openCount(F.slots) == 4)
    }

    @Test("A selection on the day that is not an open time is flagged")
    func offList() {
        let utc = TimeZone(identifier: "UTC") ?? .gmt
        let calendar = F.calendar
        #expect(Tokens.isSelectionOffList(F.at(11), slots: F.slots, day: F.day, calendar: calendar, timeZone: utc))
        #expect(!Tokens.isSelectionOffList(F.at(10), slots: F.slots, day: F.day, calendar: calendar, timeZone: utc))
        #expect(!Tokens.isSelectionOffList(F.at(30), slots: F.slots, day: F.day, calendar: calendar, timeZone: utc))
        #expect(!Tokens.isSelectionOffList(nil, slots: F.slots, day: F.day, calendar: calendar, timeZone: utc))
        #expect(!Tokens.isSelectionOffList(F.at(11), slots: F.slots, day: nil, calendar: calendar, timeZone: utc))
    }

    @Test("The phase follows day, loading, error and slots, and is announced")
    func phase() {
        #expect(Tokens.phase(hasDay: false, isLoading: true, hasError: true, slotCount: 3) == .chooseDay)
        #expect(Tokens.phase(hasDay: true, isLoading: true, hasError: true, slotCount: 3) == .loading)
        #expect(Tokens.phase(hasDay: true, isLoading: false, hasError: true, slotCount: 3) == .failed)
        #expect(Tokens.phase(hasDay: true, isLoading: false, hasError: false, slotCount: 0) == .empty)
        #expect(Tokens.phase(hasDay: true, isLoading: false, hasError: false, slotCount: 3) == .times)
        let day = "Wednesday, November 4"
        #expect(Tokens.announcement(.loading, openCount: 0, dayTitle: day, labels: none) == "Loading available times")
        #expect(Tokens.announcement(.times, openCount: 4, dayTitle: day, labels: none) == "4 times on \(day)")
        #expect(Tokens.announcement(.times, openCount: 1, dayTitle: day, labels: none) == "1 time on \(day)")
        #expect(
            Tokens.announcement(.empty, openCount: 0, dayTitle: day, labels: none) == "No times available on \(day)"
        )
        #expect(Tokens.announcement(.failed, openCount: 0, dayTitle: day, labels: none) == nil)
        let custom = MonarchSlotPickerLabels(loaded: { "\($0) @ \($1)" })
        #expect(Tokens.announcement(.times, openCount: 2, dayTitle: "D", labels: custom) == "2 @ D")
    }

    @Test("A time is read with the places left")
    func spoken() {
        #expect(Tokens.slotLabel(time: "1:30 PM", seatsLeft: 2, labels: none) == "1:30 PM, 2 left")
        #expect(Tokens.slotLabel(time: "9:00 AM", seatsLeft: nil, labels: none) == "9:00 AM")
        #expect(Tokens.seatsText(1, labels: MonarchSlotPickerLabels(seatsLeft: { "\($0) Platz" })) == "1 Platz")
    }

    @Test("Zone notes name the zone; the converted note only shows for another zone")
    func zones() {
        #expect(Tokens.zoneName("Not/AZone", locale: locale) == "Not/AZone")
        let winter = Date(timeIntervalSince1970: 1_793_836_800)
        #expect(
            Tokens.zoneName("Europe/Paris", locale: Locale(identifier: "en-GB"), at: winter)
                == "Central European Standard Time (Europe/Paris)"
        )
        #expect(Tokens.shownInText("Europe/Paris", locale: locale, labels: none).hasPrefix("Times shown in "))
        #expect(Tokens.convertedText(business: "UTC", timeZone: "UTC", locale: locale, labels: none) == nil)
        #expect(Tokens.convertedText(business: nil, timeZone: "UTC", locale: locale, labels: none) == nil)
        let converted = Tokens.convertedText(
            business: "America/Los_Angeles",
            timeZone: "UTC",
            locale: locale,
            labels: none
        )
        #expect(converted?.hasPrefix("Times are converted from ") == true)
    }

    @Test("Built-in strings resolve")
    func strings() {
        let all = [
            MonarchSlotPickerStrings.chooseDay, MonarchSlotPickerStrings.empty, MonarchSlotPickerStrings.loading,
            MonarchSlotPickerStrings.timeZone, MonarchSlotPickerStrings.chooseOutside, MonarchSlotPickerStrings.retry,
            MonarchSlotPickerStrings.times("d"), MonarchSlotPickerStrings.seatsLeft(2),
            MonarchSlotPickerStrings.shownIn("z"), MonarchSlotPickerStrings.convertedFrom("z"),
            MonarchSlotPickerStrings.notOpen("t"), MonarchSlotPickerStrings.loaded(3, "d"),
        ]
        for string in all {
            #expect(!string.hasPrefix("monarch."))
        }
    }
}
