import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchEventPopover``.
enum MonarchEventPopoverTokens {
    /// Padding around the content.
    static let padding: CGFloat = MonarchTokens.Space.space4
    /// Space between the blocks: head, details, note, actions.
    static let sectionSpacing: CGFloat = MonarchTokens.Space.space3
    /// Space between detail rows.
    static let rowSpacing: CGFloat = MonarchTokens.Space.space2
    /// Space between an icon or label and its value, and around the swatch.
    static let inlineSpacing: CGFloat = MonarchTokens.Space.space2
    /// Space between the title and the subtitle.
    static let titleSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The width of the colour swatch before the title.
    static let swatchWidth: CGFloat = MonarchTokens.Space.space1
    /// The radius of the swatch.
    static let swatchRadius: CGFloat = MonarchTokens.Radius.xs
    /// Padding inside the note.
    static let notePadding: CGFloat = MonarchTokens.Space.space2
    /// The width the popover asks for: half the small breakpoint, 320 points.
    static let idealWidth: CGFloat = MonarchTokens.Breakpoint.sm / 2

    /// The date line and the time line of the time row.
    struct When: Equatable {
        /// The day, or the start day and time when the event spans days.
        let date: String
        /// The times and zone, the end when the event spans days, or "All day".
        let time: String
    }

    /// The tone of the status badge, as on the web.
    static func tone(_ status: MonarchEventChipStatus) -> MonarchTone {
        switch status {
        case .confirmed: .success
        case .tentative: .warning
        case .cancelled: .neutral
        case .declined: .danger
        }
    }

    /// The text of the status badge, with the override applied.
    static func statusName(_ status: MonarchEventChipStatus, labels: MonarchEventPopoverLabels) -> String {
        labels.status ?? MonarchEventChipStrings.status(status)
    }

    /// Whether the title is struck through: cancelled and declined events,
    /// as in the event chip.
    static func isStruckThrough(_ status: MonarchEventChipStatus?) -> Bool {
        status.map(MonarchEventChipTokens.isStruckThrough) ?? false
    }

    /// The spoken name of the time row, with the override applied.
    static func timeLabel(labels: MonarchEventPopoverLabels) -> String {
        labels.time ?? MonarchEventPopoverStrings.time
    }

    /// "All day", with the override applied.
    static func allDayLabel(labels: MonarchEventPopoverLabels) -> String {
        labels.allDay ?? MonarchEventPopoverStrings.allDay
    }

    /// The date and time lines, formatted in `timeZone` and `locale`, as the
    /// web: "Sat, Nov 14, 2026" and "9:30 AM – 10:15 AM GMT". An all-day
    /// event shows its day or day range and "All day". An event that ends on
    /// another day shows the start day and time, then the end day and time.
    /// An end before the start is ignored.
    static func when(
        start: Date,
        end: Date?,
        isAllDay: Bool,
        timeZone: TimeZone,
        locale: Locale,
        allDayLabel: String
    ) -> When {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = timeZone
        calendar.locale = locale
        let day = Date.FormatStyle(locale: locale, calendar: calendar, timeZone: timeZone)
            .weekday(.abbreviated)
            .day()
            .month(.abbreviated)
            .year()
        let time = Date.FormatStyle(locale: locale, calendar: calendar, timeZone: timeZone)
            .monarchTime()
        // "GMT", not the offset form "GMT+0": the short standard name of the zone.
        var zoneText = timeZone.localizedName(for: .shortStandard, locale: locale) ?? timeZone.abbreviation() ?? ""
        if timeZone.secondsFromGMT(for: start) == 0, zoneText.hasPrefix("GMT") || zoneText == "UTC" {
            zoneText = "GMT"
        }
        let validEnd = end.flatMap { $0 >= start ? $0 : nil }
        let startDay = start.formatted(day)

        if isAllDay {
            guard let validEnd, !calendar.isDate(validEnd, inSameDayAs: start) else {
                return When(date: startDay, time: allDayLabel)
            }
            return When(date: "\(startDay) – \(validEnd.formatted(day))", time: allDayLabel)
        }
        let zoneName = zoneText
        guard let validEnd else {
            return When(date: startDay, time: "\(start.formatted(time)) \(zoneName)")
        }
        if calendar.isDate(validEnd, inSameDayAs: start) {
            return When(date: startDay, time: "\(start.formatted(time)) – \(validEnd.formatted(time)) \(zoneName)")
        }
        return When(
            date: "\(startDay) \(start.formatted(time)) –",
            time: "\(validEnd.formatted(day)) \(validEnd.formatted(time)) \(zoneName)"
        )
    }

    /// What VoiceOver reads for the time row: the label, then the two lines.
    static func timeAccessibility(_ when: When, labels: MonarchEventPopoverLabels) -> (label: String, value: String) {
        (timeLabel(labels: labels), "\(when.date), \(when.time)")
    }

    /// The colour role of the swatch when the app gives no colour.
    static var defaultSwatchRole: MonarchColorRole { \.textMuted }
}
