import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic of ``MonarchSlotPicker``.
enum MonarchSlotPickerTokens {
    /// The gap between the parts of the picker.
    static let spacing: CGFloat = MonarchTokens.Space.space4
    /// The gap between the times and inside a time.
    static let gridSpacing: CGFloat = MonarchTokens.Space.space2
    /// The narrowest a time button gets before the grid uses fewer columns, at the default text size.
    static let minimumSlotWidth: CGFloat = MonarchTokens.Space.space24 + MonarchTokens.Space.space2
    /// How many placeholder times show while loading.
    static let placeholderCount = 6
    /// The toggle variant of a time: bordered while off, so every time reads as a button.
    static let slotVariant: MonarchToggleButtonVariant = .outline
    /// The notes under the times.
    static var noteText: MonarchColorRole { \.textMuted }

    // MARK: Days

    /// Whether a day cannot be chosen: not in `availableDays` when given, or disabled by the app.
    static func isDayDisabled(
        _ day: Date,
        availableDays: [Date]?,
        calendar: Calendar,
        custom: Bool
    ) -> Bool {
        if custom { return true }
        guard let availableDays else { return false }
        return !availableDays.contains { calendar.isDate($0, inSameDayAs: day) }
    }

    /// The day as a heading: "Wednesday, November 4".
    static func dayTitle(_ day: Date, calendar: Calendar, locale: Locale) -> String {
        var style = Date.FormatStyle(date: .omitted, time: .omitted, locale: locale, calendar: calendar)
        style.timeZone = calendar.timeZone
        return day.formatted(style.weekday(.wide).month(.wide).day())
    }

    // MARK: Times

    /// A time in the display zone: "9:30 AM".
    static func timeText(_ date: Date, timeZone: TimeZone, calendar: Calendar, locale: Locale) -> String {
        var style = Date.FormatStyle(date: .omitted, time: .omitted, locale: locale, calendar: calendar).monarchTime()
        style.timeZone = timeZone
        return date.formatted(style)
    }

    /// The index of the selected slot, or `nil`.
    static func selectedIndex(_ selection: Date?, slots: [MonarchSlotPickerSlot]) -> Int? {
        guard let selection else { return nil }
        return slots.firstIndex { $0.start == selection }
    }

    /// The selection after pressing a slot: the slot, or nothing when the
    /// selected slot is pressed again and deselecting is allowed.
    static func selection(
        afterPressing slot: MonarchSlotPickerSlot,
        current: Date?,
        allowsDeselect: Bool
    ) -> Date? {
        guard !slot.isDisabled else { return current }
        if current == slot.start { return allowsDeselect ? nil : slot.start }
        return slot.start
    }

    /// The number of open times.
    static func openCount(_ slots: [MonarchSlotPickerSlot]) -> Int {
        slots.filter { !$0.isDisabled }.count
    }

    /// Whether the selection falls on the chosen day (in the display zone)
    /// but is not one of its open times, so a warning is shown.
    static func isSelectionOffList(
        _ selection: Date?,
        slots: [MonarchSlotPickerSlot],
        day: Date?,
        calendar: Calendar,
        timeZone: TimeZone
    ) -> Bool {
        guard let selection, let day, selectedIndex(selection, slots: slots) == nil else { return false }
        var zoned = calendar
        zoned.timeZone = timeZone
        let selectedDay = zoned.dateComponents([.year, .month, .day], from: selection)
        let shownDay = calendar.dateComponents([.year, .month, .day], from: day)
        return selectedDay == shownDay
    }

    /// What VoiceOver reads for a time: the time and the places left.
    static func slotLabel(time: String, seatsLeft: Int?, labels: MonarchSlotPickerLabels) -> String {
        guard let seatsLeft else { return time }
        return "\(time), \(seatsText(seatsLeft, labels: labels))"
    }

    /// The places-left text.
    static func seatsText(_ count: Int, labels: MonarchSlotPickerLabels) -> String {
        labels.seatsLeft?(count) ?? MonarchSlotPickerStrings.seatsLeft(count)
    }

    // MARK: Status

    /// The phase of the times area.
    enum Phase: Equatable {
        case chooseDay, loading, failed, empty, times
    }

    /// The phase from the inputs: no day, loading, an error, no slots, or times.
    static func phase(hasDay: Bool, isLoading: Bool, hasError: Bool, slotCount: Int) -> Phase {
        if !hasDay { return .chooseDay }
        if isLoading { return .loading }
        if hasError { return .failed }
        return slotCount == 0 ? .empty : .times
    }

    /// What is announced for a phase: loading, or how many times the day has. Nothing otherwise.
    static func announcement(
        _ phase: Phase,
        openCount: Int,
        dayTitle: String,
        labels: MonarchSlotPickerLabels
    ) -> String? {
        switch phase {
        case .loading: labels.loading ?? MonarchSlotPickerStrings.loading
        case .empty, .times:
            labels.loaded?(openCount, dayTitle) ?? MonarchSlotPickerStrings.loaded(openCount, dayTitle)
        case .chooseDay, .failed: nil
        }
    }

    // MARK: Zones

    /// A zone's display name at a moment, as the web writes it: the long name in force then and the id, such as
    /// "Central European Standard Time (Europe/Paris)"; Apple prints UK winter time as "GMT+00:00", or the id alone when the zone is unknown.
    static func zoneName(_ identifier: String, locale: Locale, at date: Date = Date()) -> String {
        guard let zone = TimeZone(identifier: identifier) else { return identifier }
        // The long specific name ("zzzz"), as the web's long time zone name is.
        let formatter = DateFormatter()
        formatter.locale = locale
        formatter.timeZone = zone
        formatter.dateFormat = "zzzz"
        var name = formatter.string(from: date)
        // Apple writes some winter zones as "GMT+00:00"; the zone's own long name is the one the web shows.
        if name.hasPrefix("GMT") {
            let style: NSTimeZone.NameStyle = zone.isDaylightSavingTime(for: date) ? .daylightSaving : .standard
            // The regional locale can print the same "GMT" again; the language's own name is the long one.
            let language = Locale(identifier: locale.language.languageCode?.identifier ?? "en")
            for candidate in [locale, language] {
                if let long = zone.localizedName(for: style, locale: candidate), !long.hasPrefix("GMT") {
                    name = long
                    break
                }
            }
        }
        // Some systems have no long name for winter time at zero offset: it is Greenwich Mean Time, as the web says.
        if name.hasPrefix("GMT"), zone.secondsFromGMT(for: date) == 0, !zone.isDaylightSavingTime(for: date),
            locale.language.languageCode?.identifier == "en"
        {
            name = "Greenwich Mean Time"
        }
        return name.isEmpty ? identifier : "\(name) (\(identifier))"
    }

    /// The zone note when the zone cannot be changed.
    static func shownInText(
        _ identifier: String,
        locale: Locale,
        at date: Date = Date(),
        labels: MonarchSlotPickerLabels
    ) -> String {
        let name = zoneName(identifier, locale: locale, at: date)
        return labels.shownIn?(name) ?? MonarchSlotPickerStrings.shownIn(name)
    }

    /// The conversion note, only when the business zone differs from the display zone.
    static func convertedText(
        business: String?,
        timeZone: String,
        locale: Locale,
        at date: Date = Date(),
        labels: MonarchSlotPickerLabels
    ) -> String? {
        guard let business, business != timeZone else { return nil }
        let name = zoneName(business, locale: locale, at: date)
        return labels.convertedFrom?(name) ?? MonarchSlotPickerStrings.convertedFrom(name)
    }
}
