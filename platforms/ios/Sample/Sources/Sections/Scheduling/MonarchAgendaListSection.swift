import MonarchUI
import SwiftUI

struct MonarchAgendaListSection: CataloguePage {
    static let title = "Agenda list"

    @Environment(\.monarchTheme) private var theme
    @State private var opened = "Tap an event"

    private var events: [MonarchAgendaEvent] {
        let calendar = Calendar.current
        let today = calendar.startOfDay(for: Date())
        func at(_ day: Int, _ hour: Int, _ minute: Int = 0) -> Date {
            let date = calendar.date(byAdding: .day, value: day, to: today) ?? today
            return calendar.date(bySettingHour: hour, minute: minute, second: 0, of: date) ?? date
        }
        return [
            MonarchAgendaEvent("Stand-up", id: "1", start: at(0, 9, 30), end: at(0, 9, 45)),
            MonarchAgendaEvent("Design review", id: "2", start: at(0, 14), end: at(0, 15), detail: "Room 4"),
            MonarchAgendaEvent("Offsite", id: "3", start: today, end: at(1, 0), isAllDay: true, status: .tentative),
            MonarchAgendaEvent("Release night", id: "4", start: at(1, 22), end: at(3, 2), detail: "On call: Ada"),
            MonarchAgendaEvent("Supplier call", id: "5", start: at(4, 11), end: at(4, 11, 30), status: .cancelled),
        ]
    }

    var body: some View {
        Text(opened).monarchTextStyle(.bodyStrong)
        MonarchAgendaList(
            events,
            dayNotes: [Date().addingTimeInterval(86_400): ["Grace is on holiday"]]
        ) { event in
            opened = "Opened \(event.title)"
        }
        .frame(height: theme.spacing.space24 * 5)

        CatalogueCaption("Nothing scheduled")
        MonarchAgendaList([])
            .frame(height: theme.spacing.space24 * 3)
    }
}
