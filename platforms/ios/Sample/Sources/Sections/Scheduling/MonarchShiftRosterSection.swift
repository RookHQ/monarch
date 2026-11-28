@_spi(Experimental) import MonarchUI
import SwiftUI

struct MonarchShiftRosterSection: CataloguePage {
    static let title = "Shift roster"

    @State private var roster: [String: [Date: MonarchShiftRosterAssignment]] = [:]
    @State private var layout = MonarchShiftRosterLayout.byDay
    @State private var week = 0
    @State private var view = MonarchCalendarToolbarView.week
    @State private var isSaving = false
    @State private var lastChange = "No changes yet"

    private let calendar = Calendar.current

    private var monday: Date {
        let today = calendar.startOfDay(for: Date())
        let start = calendar.dateInterval(of: .weekOfYear, for: today)?.start ?? today
        return calendar.date(byAdding: .weekOfYear, value: week, to: start) ?? start
    }

    private let members = [
        MonarchShiftRosterMember(id: "a", name: "Alex Kim", description: "Support"),
        MonarchShiftRosterMember(id: "b", name: "Sam Lee", description: "Support"),
        MonarchShiftRosterMember(id: "c", name: "Priya Patel", description: "Team lead"),
    ]

    private let shifts = [
        MonarchShiftRosterShift(id: "m", name: "Morning", start: 6 * 60, end: 14 * 60, shortName: "M"),
        MonarchShiftRosterShift(id: "e", name: "Evening", start: 14 * 60, end: 22 * 60, shortName: "E"),
        MonarchShiftRosterShift(id: "n", name: "Night", start: 22 * 60, end: 6 * 60, shortName: "N"),
    ]

    var body: some View {
        MonarchCalendarToolbar(
            "Week of " + monday.formatted(.dateTime.day().month(.abbreviated)),
            view: $view,
            views: [.week],
            isTodayDisabled: week == 0,
            onToday: { week = 0 },
            onPrevious: { week -= 1 },
            onNext: { week += 1 }
        )
        Picker("Layout", selection: $layout) {
            Text("Day").tag(MonarchShiftRosterLayout.byDay)
            Text("Person").tag(MonarchShiftRosterLayout.byPerson)
            Text("Grid").tag(MonarchShiftRosterLayout.grid)
            Text("Auto").tag(MonarchShiftRosterLayout.automatic)
        }
        .pickerStyle(.segmented)
        Toggle("Saving (cells disabled)", isOn: $isSaving).monarchTextStyle(.body)
        Text(lastChange).monarchTextStyle(.caption)
        MonarchShiftRoster(
            members: members,
            shifts: shifts,
            from: monday,
            value: $roster,
            layout: layout,
            calendar: calendar,
            onChange: { change in
                lastChange = "Saved \(change.memberId) on " + change.day.formatted(date: .abbreviated, time: .omitted)
            }
        )
        .disabled(isSaving)

        CatalogueCaption("Read only, without totals")
        MonarchShiftRoster(
            members: members,
            shifts: shifts,
            from: monday,
            value: .constant(["a": [monday: .shift("m")], "b": [monday: .off]]),
            days: 3,
            layout: .byPerson,
            isReadOnly: true,
            showsTotals: false,
            calendar: calendar
        )

        CatalogueCaption("Loading, error and empty")
        MonarchShiftRoster(members: members, shifts: shifts, from: monday, value: $roster, isLoading: true)
        MonarchShiftRoster(
            members: members,
            shifts: shifts,
            from: monday,
            value: $roster,
            error: "The roster could not be loaded."
        )
        MonarchShiftRoster(members: [], shifts: shifts, from: monday, value: $roster)
    }
}
