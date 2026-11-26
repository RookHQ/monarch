import Foundation

/// Everything the grid of a team calendar shows, worked out once per update.
struct MonarchTeamCalendarModel {
    /// The days shown, at the start of each day.
    let days: [Date]
    /// Whether each day's column is shaded.
    let shaded: [Bool]
    /// The holidays by the start of their day.
    let holidays: [Date: String]
    /// The index of today in `days`, or `nil` when today is not shown.
    let todayIndex: Int?
    /// The index of the day the days start scrolled to.
    let scrollTarget: Int?
    /// What each day holds, by member id.
    let cells: [String: [MonarchTeamCalendarCell?]]
    /// The bars of each row, by member id.
    let segments: [String: [MonarchTeamCalendarSegment]]
    /// The name of the range: the month, or the custom range.
    let rangeName: String

    static func make(
        members: [MonarchTeamCalendarMember],
        entries: [MonarchTeamCalendarEntry],
        month: Date?,
        from: Date?,
        to: Date?,
        holidays: [MonarchTeamCalendarHoliday],
        weekendDays: Set<Int>?,
        today: Date,
        formats: MonarchTeamCalendarFormats
    ) -> MonarchTeamCalendarModel {
        typealias Tokens = MonarchTeamCalendarTokens
        let calendar = formats.calendar
        let range = Tokens.range(month: month, from: from, to: to, today: today, calendar: calendar)
        let days = Tokens.days(from: range.first, through: range.last, calendar: calendar)
        let names = Tokens.holidayNames(holidays, calendar: calendar)
        var cells: [String: [MonarchTeamCalendarCell?]] = [:]
        var segments: [String: [MonarchTeamCalendarSegment]] = [:]
        for member in members {
            let own = Tokens.cells(Tokens.entries(of: member.id, in: entries), days: days, calendar: calendar)
            cells[member.id] = own
            segments[member.id] = Tokens.segments(own, days: days, calendar: calendar)
        }
        return MonarchTeamCalendarModel(
            days: days,
            shaded: days.map { Tokens.isShaded($0, weekendDays: weekendDays, holidays: names, calendar: calendar) },
            holidays: names,
            todayIndex: days.firstIndex { calendar.isDate($0, inSameDayAs: today) },
            scrollTarget: Tokens.scrollTarget(today: today, days: days, calendar: calendar),
            cells: cells,
            segments: segments,
            rangeName: formats.rangeName(first: range.first, last: range.last, isCustom: from != nil && to != nil)
        )
    }
}
