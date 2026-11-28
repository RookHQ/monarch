import Foundation
import MonarchTokens
import SwiftUI

/// A badge of an entry: its text, tone and variant.
struct MonarchTimeOffBadge: Hashable {
    /// The text.
    let text: String
    /// The tone.
    let tone: MonarchTone
    /// The variant.
    let variant: MonarchBadgeVariant
}

/// The values of the add form, before they become a draft.
struct MonarchTimeOffForm: Equatable {
    /// The chosen person, or `nil` for everyone.
    var personID: String?
    /// The chosen type, or `nil`.
    var type: String?
    /// Whether the entry is whole days.
    var isAllDay = true
    /// The first day.
    var fromDay: Date
    /// The last day, included.
    var untilDay: Date
    /// The start moment.
    var start: Date
    /// The end moment.
    var end: Date
    /// The reason as typed.
    var reason = ""
}

/// Component tokens and pure logic for ``MonarchTimeOffPanel``.
enum MonarchTimeOffPanelTokens {
    /// The gap between the parts of the panel.
    static let sectionSpacing: CGFloat = MonarchTokens.Space.space4
    /// The gap between the lines of an entry and between badges.
    static let lineSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between an entry's text and its remove button.
    static let rowSpacing: CGFloat = MonarchTokens.Space.space3
    /// The space above and below an entry.
    static let rowPadding: CGFloat = MonarchTokens.Space.space2
    /// The number of placeholder rows while loading.
    static let loadingRows = 3
    /// The longest reason, in characters.
    static let maximumReasonLength = 200
    /// The hour a new timed entry starts.
    static let defaultStartHour = 9
    /// The hour a new timed entry ends.
    static let defaultEndHour = 17
    /// The add button's symbol.
    static let addSymbol = "plus"
    /// The remove button's symbol.
    static let removeSymbol = "trash"
    /// The colour of the description, the dates and the time zone.
    static var mutedRole: MonarchColorRole { \.textMuted }
    /// The colour of the name.
    static var textRole: MonarchColorRole { \.textDefault }
    /// The colour of the form's error.
    static var errorRole: MonarchColorRole { \.dangerStrong }

    // MARK: Entries

    /// Who an entry is for: the person's name, or "Everyone" for a closure.
    static func who(_ entry: MonarchTimeOffEntry, labels: MonarchTimeOffPanelLabels) -> String {
        entry.personID == nil ? labels.everyone ?? MonarchTimeOffPanelStrings.everyone : entry.personName ?? ""
    }

    /// Whether an entry has ended: an all-day entry before today, a timed one before now.
    static func isPast(_ entry: MonarchTimeOffEntry, now: Date, calendar: Calendar) -> Bool {
        if entry.isAllDay {
            return calendar.startOfDay(for: entry.end) < calendar.startOfDay(for: now)
        }
        return entry.end < now
    }

    /// The entries shown: all of them, or those that have not ended.
    static func visible(_ entries: [MonarchTimeOffEntry], showsPast: Bool, now: Date, calendar: Calendar)
        -> [MonarchTimeOffEntry]
    {
        showsPast ? entries : entries.filter { !isPast($0, now: now, calendar: calendar) }
    }

    /// The badges of an entry: closure, type, status, past.
    static func badges(_ entry: MonarchTimeOffEntry, isPast: Bool, labels: MonarchTimeOffPanelLabels)
        -> [MonarchTimeOffBadge]
    {
        var result: [MonarchTimeOffBadge] = []
        if entry.personID == nil {
            result.append(
                MonarchTimeOffBadge(
                    text: labels.closure ?? MonarchTimeOffPanelStrings.closure,
                    tone: .warning,
                    variant: .subtle
                )
            )
        }
        if let type = entry.type, !type.isEmpty {
            result.append(MonarchTimeOffBadge(text: type, tone: .neutral, variant: .subtle))
        }
        if let status = entry.status, !status.isEmpty {
            result.append(MonarchTimeOffBadge(text: status, tone: .info, variant: .subtle))
        }
        if isPast {
            result.append(
                MonarchTimeOffBadge(
                    text: labels.past ?? MonarchTimeOffPanelStrings.past,
                    tone: .neutral,
                    variant: .outline
                )
            )
        }
        return result
    }

    /// When an entry is: "Mon, Nov 9, 2026 – Fri, Nov 13, 2026 (all day)", or
    /// "Mon, Nov 9, 2026, 9:00 AM – 5:00 PM", in the calendar's time zone.
    static func when(
        _ entry: MonarchTimeOffEntry,
        labels: MonarchTimeOffPanelLabels,
        locale: Locale,
        calendar: Calendar
    )
        -> String
    {
        let day = Date.FormatStyle(locale: locale, calendar: calendar, timeZone: calendar.timeZone)
            .weekday(.abbreviated).day().month(.abbreviated).year()
        let startDay = day.format(entry.start)
        let endDay = day.format(entry.end)
        if entry.isAllDay {
            let allDay = labels.allDay ?? MonarchTimeOffPanelStrings.allDay
            return startDay == endDay ? "\(startDay) (\(allDay))" : "\(startDay) – \(endDay) (\(allDay))"
        }
        // A 24-hour clock pads the hour ("09:00 – 13:00").
        let clock = Date.FormatStyle(locale: locale, calendar: calendar, timeZone: calendar.timeZone)
        let pattern = DateFormatter.dateFormat(fromTemplate: "j", options: 0, locale: locale) ?? ""
        let time =
            pattern.contains("H") || pattern.contains("k")
            ? clock.hour(.twoDigits(amPM: .omitted)).minute(.twoDigits) : clock.monarchTime()
        let startTime = time.format(entry.start)
        let endTime = time.format(entry.end)
        return startDay == endDay
            ? "\(startDay), \(startTime) – \(endTime)"
            : "\(startDay), \(startTime) – \(endDay), \(endTime)"
    }

    /// The dates line: when, then " · reason".
    static func detail(_ when: String, reason: String?) -> String {
        guard let reason, !reason.isEmpty else { return when }
        return "\(when) · \(reason)"
    }

    /// What VoiceOver reads for an entry: who, the badges, then the dates and reason.
    static func summary(who: String, badges: [MonarchTimeOffBadge], detail: String) -> String {
        ([who] + badges.map(\.text) + [detail]).filter { !$0.isEmpty }.joined(separator: ", ")
    }

    /// The name of an entry's remove button.
    static func removeLabel(who: String, when: String, labels: MonarchTimeOffPanelLabels) -> String {
        labels.remove?(who, when) ?? MonarchTimeOffPanelStrings.remove(who, when)
    }

    /// The message of the remove confirmation: "Who · when. Why it matters."
    static func removeMessage(who: String, when: String, labels: MonarchTimeOffPanelLabels) -> String {
        "\(who) · \(when). \(labels.removeMessage ?? MonarchTimeOffPanelStrings.removeMessage)"
    }

    /// Whether an entry shows a remove button.
    static func isRemovable(
        _ entry: MonarchTimeOffEntry,
        hasRemove: Bool,
        canRemove: ((MonarchTimeOffEntry) -> Bool)?
    ) -> Bool {
        hasRemove && (canRemove?(entry) ?? true)
    }

    /// The message of a failed add or remove: the error's description, or the built-in one.
    static func failure(_ error: any Error, labels: MonarchTimeOffPanelLabels) -> String {
        MonarchFailureMessage.message(for: error, fallback: labels.failed ?? MonarchTimeOffPanelStrings.failed)
    }

    /// Where VoiceOver focus can be put.
    enum FocusTarget: Hashable {
        /// The panel's heading, when no row is left.
        case heading
        /// The row of the entry with this id.
        case row(String)
    }

    /// Where focus goes after an entry is removed: the entry now at its
    /// position, else the one before it, else the heading.
    static func focusAfterRemoval(removing id: String, visibleIDs: [String]) -> FocusTarget {
        MonarchFocusAfterRemoval.target(removing: id, from: visibleIDs).map(FocusTarget.row) ?? .heading
    }

    // MARK: Form

    /// The people "Who" offers: the app's, then Everyone when closures are allowed.
    static func whoOptions(people: [MonarchTimeOffOption], allowsClosure: Bool) -> [MonarchTimeOffOption?] {
        people.map { Optional($0) } + (allowsClosure ? [nil] : [])
    }

    /// The form when it opens: the first person (or everyone), the first
    /// type, whole days from today, and 9:00 to 17:00 today for timed entries.
    static func initialForm(
        people: [MonarchTimeOffOption],
        types: [MonarchTimeOffOption],
        now: Date,
        calendar: Calendar
    ) -> MonarchTimeOffForm {
        let today = calendar.startOfDay(for: now)
        return MonarchTimeOffForm(
            personID: people.first?.value,
            type: types.first?.value,
            fromDay: today,
            untilDay: today,
            start: calendar.date(bySettingHour: defaultStartHour, minute: 0, second: 0, of: today) ?? today,
            end: calendar.date(bySettingHour: defaultEndHour, minute: 0, second: 0, of: today) ?? today
        )
    }

    /// The form after the first day changes: the last day follows when it would fall before it.
    static func settingFromDay(_ day: Date, form: MonarchTimeOffForm, calendar: Calendar) -> MonarchTimeOffForm {
        var result = form
        result.fromDay = calendar.startOfDay(for: day)
        if result.untilDay < result.fromDay { result.untilDay = result.fromDay }
        return result
    }

    /// The problem of the form, or `nil`: an all-day end before its start, a timed end not after its start.
    static func problem(_ form: MonarchTimeOffForm, labels: MonarchTimeOffPanelLabels, calendar: Calendar) -> String? {
        let isWrong =
            form.isAllDay
            ? calendar.startOfDay(for: form.untilDay) < calendar.startOfDay(for: form.fromDay)
            : form.end <= form.start
        return isWrong ? labels.endBeforeStart ?? MonarchTimeOffPanelStrings.endBeforeStart : nil
    }

    /// The draft of a valid form: whole days as the first moment of each day,
    /// the type only when types are offered, the reason trimmed or `nil`.
    static func draft(_ form: MonarchTimeOffForm, offersTypes: Bool, calendar: Calendar) -> MonarchTimeOffDraft {
        let reason = form.reason.trimmingCharacters(in: .whitespacesAndNewlines)
        return MonarchTimeOffDraft(
            personID: form.personID,
            isAllDay: form.isAllDay,
            start: form.isAllDay ? calendar.startOfDay(for: form.fromDay) : form.start,
            end: form.isAllDay ? calendar.startOfDay(for: form.untilDay) : form.end,
            type: offersTypes ? form.type : nil,
            reason: reason.isEmpty ? nil : reason
        )
    }

    /// The reason kept to the longest allowed length.
    static func limited(_ reason: String) -> String {
        reason.count > maximumReasonLength ? String(reason.prefix(maximumReasonLength)) : reason
    }

    /// The note under the dates: the built-in sentence and the time zone's name.
    static func zoneNote(_ timeZone: TimeZone, labels: MonarchTimeOffPanelLabels, locale: Locale) -> String {
        let name = timeZone.localizedName(for: .generic, locale: locale) ?? timeZone.identifier
        return "\(labels.formMessage ?? MonarchTimeOffPanelStrings.formMessage) \(name)"
    }
}
