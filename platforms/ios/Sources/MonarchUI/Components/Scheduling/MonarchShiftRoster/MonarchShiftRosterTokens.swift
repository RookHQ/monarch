import Foundation
import MonarchTokens
import SwiftUI

/// A roster value: assignments by member id, then by any time on the day.
typealias MonarchShiftRosterValue = [String: [Date: MonarchShiftRosterAssignment]]

/// The colours of a cell that has no shift.
struct MonarchShiftRosterSlotAppearance: Equatable {
    /// The fill, or `nil` for none.
    let fill: MonarchColorRole?
    /// The text.
    let content: MonarchColorRole
    /// The dashed outline, or `nil` for none.
    let border: MonarchColorRole?
}

/// Component tokens and pure rules of ``MonarchShiftRoster``.
enum MonarchShiftRosterTokens {
    // MARK: Sizes

    /// The gap between the tabs, the summary and the rows.
    static let sectionSpacing: CGFloat = MonarchTokens.Space.space3
    /// The padding above and below a row of the one-day and one-person layouts.
    static let rowPadding: CGFloat = MonarchTokens.Space.space2
    /// The gap between the person (or the day) and the cell in a row.
    static let rowSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the avatar and the name.
    static let personSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the parts of a day's summary.
    static let summarySpacing: CGFloat = MonarchTokens.Space.space2
    /// The smallest height of an Off or Not assigned slot.
    static let slotMinHeight: CGFloat = MonarchTokens.Space.space8
    /// The padding at the sides of a slot.
    static let slotHorizontalPadding: CGFloat = MonarchTokens.Space.space2
    /// The padding above and below a slot's text.
    static let slotVerticalPadding: CGFloat = MonarchTokens.Space.space1
    /// The length of a dash, and of the gap between dashes, of a Not assigned slot.
    static let dashLength: CGFloat = MonarchTokens.Space.space1
    /// The padding around a cell of the grid.
    static let cellPadding: CGFloat = MonarchTokens.Space.space1
    /// The padding of the grid's headers, people and totals.
    static let headerPadding: CGFloat = MonarchTokens.Space.space2
    /// The width of the people column of the grid, before Dynamic Type scaling.
    static let personColumnWidth: CGFloat = MonarchTokens.Space.space24 + MonarchTokens.Space.space16
    /// The width of a day column of the grid, before Dynamic Type scaling.
    static let dayColumnWidth: CGFloat = MonarchTokens.Space.space24 + MonarchTokens.Space.space4
    /// The width of the hours column of the grid, before Dynamic Type scaling.
    static let hoursColumnWidth: CGFloat = MonarchTokens.Space.space20
    /// The corner of a slot.
    static var slotShape: RoundedRectangle { MonarchShapes.mark }

    // MARK: Text and symbols

    /// The type role of names and dates in rows.
    static let nameStyle: MonarchTextStyle = .bodyStrong
    /// The type role of the day heading under the day tabs.
    static let summaryStyle: MonarchTextStyle = .subheading
    /// The type role of grid headers.
    static let headerStyle: MonarchTextStyle = .label
    /// The type role of descriptions, totals, counts and slots.
    static let captionStyle: MonarchTextStyle = .caption
    /// The visible mark of a day with no assignment in the narrow cells of the grid.
    /// VoiceOver still reads "Not assigned".
    static let unassignedMark = "–"
    /// The separator between a shift's name and times in its menu item.
    static let menuSeparator = " · "
    /// The separator between a person's description and hours.
    static let supportingSeparator = " · "
    /// The SF Symbol of the empty state.
    static let emptySymbol = "person.2"
    /// The tone of the Today badge. The web uses its accent; `MonarchTone` has no accent, so info.
    static let todayTone: MonarchTone = .info
    /// The number of placeholder rows while loading.
    static let placeholderRows = 3

    // MARK: Colours

    /// The colour of names.
    static var nameColor: MonarchColorRole { \.textDefault }
    /// The colour of descriptions, totals, counts and grid headers.
    static var mutedColor: MonarchColorRole { \.textMuted }
    /// The fill of the grid's header and footer rows.
    static var headerFill: MonarchColorRole { \.bgSubtle }

    /// The colour of a day's name: the same for today, which is marked in words.
    static func dayColor(isToday _: Bool) -> MonarchColorRole {
        \.textDefault
    }

    /// A cell without a shift: a day off is a muted fill with muted text; a day that is not
    /// assigned is a dashed outline in the default border colour on no fill. Both also say
    /// what they are in words.
    static func slotAppearance(isOff: Bool) -> MonarchShiftRosterSlotAppearance {
        isOff
            ? MonarchShiftRosterSlotAppearance(fill: \.bgMuted, content: \.textMuted, border: nil)
            : MonarchShiftRosterSlotAppearance(fill: nil, content: \.textMuted, border: \.borderDefault)
    }

    // MARK: Layout rules

    /// The most days a roster shows, about two months, so a wrong count does not build
    /// thousands of cells.
    static let maxDays = 62

    /// Whether the automatic layout may show the grid at this text size: up to xxLarge, so
    /// enlarged text gets the one-day layout, where nothing is squeezed.
    static func allowsGrid(_ size: DynamicTypeSize) -> Bool {
        size <= .xxLarge
    }

    /// Whether the rows of the one-day and one-person layouts put the cell under the person
    /// or the day instead of beside it.
    static func stacksRows(_ size: DynamicTypeSize) -> Bool {
        size.isAccessibilitySize
    }

    /// The width the grid needs for its columns.
    static func gridWidth(person: CGFloat, day: CGFloat, hours: CGFloat, days: Int, showsTotals: Bool) -> CGFloat {
        person + day * CGFloat(days) + (showsTotals ? hours : 0)
    }

    // MARK: Data rules

    /// The days shown: `days` days (at least one, at most ``maxDays``) from the day of `from`.
    static func range(from: Date, days: Int, calendar: Calendar) -> [Date] {
        let first = calendar.startOfDay(for: from)
        return (0..<min(max(days, 1), maxDays)).compactMap { calendar.date(byAdding: .day, value: $0, to: first) }
    }

    /// The day the one-day layout opens on: the chosen day when it is in the range, then today
    /// when it is, otherwise the first day.
    static func shownDay(chosen: Date?, range: [Date], today: Date?, calendar: Calendar) -> Date? {
        if let chosen, range.contains(chosen) { return chosen }
        if let today, let day = range.first(where: { calendar.isDate($0, inSameDayAs: today) }) { return day }
        return range.first
    }

    /// The person the one-person layout shows: the chosen one when present, otherwise the first.
    static func shownMember(chosen: String?, members: [MonarchShiftRosterMember]) -> String? {
        if let chosen, members.contains(where: { $0.id == chosen }) { return chosen }
        return members.first?.id
    }

    /// The assignment of a person on a day, or `nil` when the day is not assigned. Keys may be
    /// any time on the day.
    static func assignment(
        in value: MonarchShiftRosterValue,
        memberId: String,
        day: Date,
        calendar: Calendar
    ) -> MonarchShiftRosterAssignment? {
        value[memberId]?
            .filter { calendar.isDate($0.key, inSameDayAs: day) }
            .max { $0.key < $1.key }?
            .value
    }

    /// `value` with `assignment` on the day for the person, or with the day removed when
    /// `assignment` is `nil`. Any other key on the same day is replaced.
    static func assigning(
        _ assignment: MonarchShiftRosterAssignment?,
        in value: MonarchShiftRosterValue,
        memberId: String,
        day: Date,
        calendar: Calendar
    ) -> MonarchShiftRosterValue {
        var row = (value[memberId] ?? [:]).filter { !calendar.isDate($0.key, inSameDayAs: day) }
        if let assignment {
            row[calendar.startOfDay(for: day)] = assignment
        }
        var next = value
        next[memberId] = row
        return next
    }

    /// The shift an assignment refers to, or `nil` for a day off, no assignment or an unknown id.
    static func shift(
        of assignment: MonarchShiftRosterAssignment?,
        in shifts: [MonarchShiftRosterShift]
    ) -> MonarchShiftRosterShift? {
        guard case .shift(let id) = assignment else { return nil }
        return shifts.first { $0.id == id }
    }

    private static let minutesPerDay = 1440

    private static func wrapped(_ minutes: Int) -> Int {
        ((minutes % minutesPerDay) + minutesPerDay) % minutesPerDay
    }

    /// Whether a shift ends the next day: its end is at or before its start.
    static func isOvernight(_ shift: MonarchShiftRosterShift) -> Bool {
        wrapped(shift.end) <= wrapped(shift.start)
    }

    /// The length of a shift in minutes. A shift that ends when it starts lasts a whole day.
    static func durationMinutes(_ shift: MonarchShiftRosterShift) -> Int {
        let start = wrapped(shift.start)
        let end = wrapped(shift.end)
        return end > start ? end - start : end + minutesPerDay - start
    }

    /// The hours a person works on the days of the range.
    static func hours(
        in value: MonarchShiftRosterValue,
        memberId: String,
        range: [Date],
        shifts: [MonarchShiftRosterShift],
        calendar: Calendar
    ) -> Double {
        let minutes = range.reduce(0) { total, day in
            let assigned = assignment(in: value, memberId: memberId, day: day, calendar: calendar)
            return total + (shift(of: assigned, in: shifts).map(durationMinutes) ?? 0)
        }
        return Double(minutes) / 60
    }

    /// How many of the people work a known shift on a day.
    static func working(
        in value: MonarchShiftRosterValue,
        members: [MonarchShiftRosterMember],
        day: Date,
        shifts: [MonarchShiftRosterShift],
        calendar: Calendar
    ) -> Int {
        members.filter {
            shift(of: assignment(in: value, memberId: $0.id, day: day, calendar: calendar), in: shifts) != nil
        }.count
    }
}
