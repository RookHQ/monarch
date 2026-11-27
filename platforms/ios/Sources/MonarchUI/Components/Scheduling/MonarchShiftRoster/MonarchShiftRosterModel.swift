import Foundation
import SwiftUI

/// Everything the layouts of ``MonarchShiftRoster`` share: the data, the state
/// flags, the strings and the formats, and the pure functions that build what
/// is shown and what VoiceOver reads.
struct MonarchShiftRosterModel {
    let members: [MonarchShiftRosterMember]
    let shifts: [MonarchShiftRosterShift]
    let range: [Date]
    let value: Binding<MonarchShiftRosterValue>
    let isReadOnly: Bool
    let showsTotals: Bool
    let today: Date?
    let calendar: Calendar
    let text: MonarchShiftRosterText
    let formats: MonarchShiftRosterFormats
    let onChange: (@MainActor (MonarchShiftRosterChange) -> Void)?
    let onSelectMember: (@MainActor (MonarchShiftRosterMember) -> Void)?

    func assignment(_ member: MonarchShiftRosterMember, on day: Date) -> MonarchShiftRosterAssignment? {
        MonarchShiftRosterTokens.assignment(in: value.wrappedValue, memberId: member.id, day: day, calendar: calendar)
    }

    func shift(of assignment: MonarchShiftRosterAssignment?) -> MonarchShiftRosterShift? {
        MonarchShiftRosterTokens.shift(of: assignment, in: shifts)
    }

    /// Sets or clears one cell, then reports the change. Does nothing when read only or when
    /// the assignment does not change.
    @MainActor func assign(
        _ assignment: MonarchShiftRosterAssignment?,
        to member: MonarchShiftRosterMember,
        on day: Date
    ) {
        guard !isReadOnly, self.assignment(member, on: day) != assignment else { return }
        value.wrappedValue = MonarchShiftRosterTokens.assigning(
            assignment,
            in: value.wrappedValue,
            memberId: member.id,
            day: day,
            calendar: calendar
        )
        onChange?(
            MonarchShiftRosterChange(memberId: member.id, day: calendar.startOfDay(for: day), assignment: assignment)
        )
    }

    func isToday(_ day: Date) -> Bool {
        today.map { calendar.isDate($0, inSameDayAs: day) } ?? false
    }

    /// "40 h".
    func hours(_ member: MonarchShiftRosterMember) -> String {
        let hours = MonarchShiftRosterTokens.hours(
            in: value.wrappedValue,
            memberId: member.id,
            range: range,
            shifts: shifts,
            calendar: calendar
        )
        return text.hoursValue(formats.hours(hours))
    }

    func working(_ day: Date) -> Int {
        MonarchShiftRosterTokens.working(
            in: value.wrappedValue,
            members: members,
            day: day,
            shifts: shifts,
            calendar: calendar
        )
    }

    /// "3 working".
    func workingValue(_ day: Date) -> String {
        let count = working(day)
        return text.workingValue(count, formatted: formats.count(count))
    }

    /// "06:00 – 14:00", as shown.
    func visibleRange(_ shift: MonarchShiftRosterShift) -> String {
        "\(formats.time(shift.start)) – \(formats.time(shift.end))"
    }

    /// "Night, 22:00 to 06:00, overnight", as VoiceOver reads a shift. `isCompact` adds the
    /// short name shown in the grid.
    func spokenShift(_ shift: MonarchShiftRosterShift, isCompact: Bool = false) -> String {
        var parts = [shift.name]
        if isCompact, let short = shift.shortName, short != shift.name { parts.append(short) }
        parts.append(text.range(formats.time(shift.start), formats.time(shift.end)))
        if MonarchShiftRosterTokens.isOvernight(shift) { parts.append(text.overnight) }
        return parts.joined(separator: ", ")
    }

    /// What an assignment reads as.
    func describe(_ assignment: MonarchShiftRosterAssignment?, isCompact: Bool) -> String {
        if let shift = shift(of: assignment) { return spokenShift(shift, isCompact: isCompact) }
        return assignment == .off ? text.off : text.unassigned
    }

    /// "Wednesday, November 4", with ", Today" for today.
    func spokenDay(_ day: Date) -> String {
        isToday(day) ? "\(formats.fullDay(day)), \(text.today)" : formats.fullDay(day)
    }

    /// The title of a day tab: "Today" or "Wed 4".
    func tabTitle(_ day: Date) -> String {
        isToday(day) ? text.today : formats.shortDay(day)
    }

    /// What VoiceOver reads for a cell: "Alex Kim, Wednesday, November 4, Today: Morning, …".
    func cellLabel(_ member: MonarchShiftRosterMember, on day: Date, isCompact: Bool) -> String {
        text.cell(member.name, spokenDay(day), describe(assignment(member, on: day), isCompact: isCompact))
    }

    /// The heading of a cell's menu: "Alex Kim, Wednesday, November 4".
    func menuTitle(_ member: MonarchShiftRosterMember, on day: Date) -> String {
        text.menuTitle(member.name, formats.fullDay(day))
    }

    /// The text of a shift's menu item: "Morning · 06:00 – 14:00".
    func menuItem(_ shift: MonarchShiftRosterShift) -> String {
        shift.name + MonarchShiftRosterTokens.menuSeparator + visibleRange(shift)
    }

    /// The muted line under a person's name: "Support · 40 h".
    func supporting(_ member: MonarchShiftRosterMember) -> String? {
        let parts = [member.description, showsTotals ? hours(member) : nil].compactMap { $0 }.filter { !$0.isEmpty }
        return parts.isEmpty ? nil : parts.joined(separator: MonarchShiftRosterTokens.supportingSeparator)
    }

    /// What VoiceOver reads for a person: "Alex Kim, Support, Hours: 40 h".
    func personLabel(_ member: MonarchShiftRosterMember) -> String {
        var parts = [member.name]
        if let description = member.description, !description.isEmpty { parts.append(description) }
        if showsTotals { parts.append(hoursLabel(member)) }
        return parts.joined(separator: ", ")
    }

    /// "Hours: 40 h".
    func hoursLabel(_ member: MonarchShiftRosterMember) -> String {
        "\(text.hours): \(hours(member))"
    }

    /// What VoiceOver reads for a count in the grid's footer: "Wednesday, November 4: 3 working".
    func workingLabel(_ day: Date) -> String {
        "\(formats.fullDay(day)): \(workingValue(day))"
    }
}
