@_spi(Experimental) import MonarchUI
import SwiftUI

struct MonarchTeamCalendarSection: CataloguePage {
    static let title = "Team calendar"

    @Environment(\.monarchTheme) private var theme
    @State private var view = MonarchCalendarToolbarView.month
    @State private var offset = 0
    @State private var status = "Tap a bar or an empty day"
    @State private var isLoading = false

    private let calendar = Calendar.current

    private var month: Date {
        calendar.date(byAdding: .month, value: offset, to: Date()) ?? Date()
    }

    /// A day of the current month.
    private func day(_ number: Int) -> Date {
        let start = calendar.dateInterval(of: .month, for: Date())?.start ?? Date()
        return calendar.date(byAdding: .day, value: number - 1, to: start) ?? start
    }

    private let members = [
        MonarchTeamCalendarMember("Alex Kim", id: "alex", description: "Design"),
        MonarchTeamCalendarMember("Sam Lee", id: "sam", description: "Engineering"),
        MonarchTeamCalendarMember("Priya Nair", id: "priya", description: "Support"),
        MonarchTeamCalendarMember("Jordan Ellis", id: "jordan"),
    ]

    private var entries: [MonarchTeamCalendarEntry] {
        [
            MonarchTeamCalendarEntry("Annual leave", id: "1", memberID: "alex", start: day(2), end: day(6)),
            MonarchTeamCalendarEntry(
                "Doctor",
                id: "2",
                memberID: "sam",
                start: day(4),
                portion: .firstHalf,
                isPending: true
            ),
            MonarchTeamCalendarEntry(
                "Conference",
                id: "3",
                memberID: "priya",
                start: day(9),
                end: day(11),
                color: theme.colors.successSolid,
                portions: [day(11): .secondHalf]
            ),
            MonarchTeamCalendarEntry(
                "Parental leave",
                id: "4",
                memberID: "jordan",
                start: day(14),
                end: day(28),
                isPending: true
            ),
        ]
    }

    var body: some View {
        MonarchCalendarToolbar(
            month.formatted(.dateTime.month(.wide).year()),
            view: $view,
            views: [.month],
            isBusy: isLoading,
            isTodayDisabled: offset == 0,
            onToday: { offset = 0 },
            onPrevious: { offset -= 1 },
            onNext: { offset += 1 }
        )
        Text(status).monarchTextStyle(.bodyStrong)
        MonarchTeamCalendar(
            members: members,
            entries: entries,
            month: month,
            holidays: [MonarchTeamCalendarHoliday("Company day off", date: day(20))],
            isLoading: isLoading,
            onEntryTap: { entry, member in status = "Opened \(entry.label) for \(member.name)" },
            onCellTap: { member, date in
                status = "Add time away for \(member.name) on \(date.formatted(date: .abbreviated, time: .omitted))"
            }
        )
        .frame(height: theme.spacing.space24 * 4)
        Toggle("Loading", isOn: $isLoading).monarchTextStyle(.body)

        CatalogueCaption("Two weeks, read only: tap a bar for its details")
        MonarchTeamCalendar(
            members: Array(members.prefix(2)),
            entries: entries,
            from: day(1),
            to: day(14),
            showsLegend: false
        )
        .frame(height: theme.spacing.space24 * 2)

        CatalogueCaption("Nobody to show")
        MonarchTeamCalendar(members: [], entries: [])
            .frame(height: theme.spacing.space24 * 3)

        CatalogueCaption("Error")
        MonarchTeamCalendar(members: members, entries: [], errorMessage: "Could not load time away.")
    }
}
