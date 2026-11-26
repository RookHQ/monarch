import MonarchUI
import SwiftUI

struct MonarchCalendarMonthViewSection: CataloguePage {
    static let title = "Calendar month view"

    @Environment(\.calendar) private var calendar
    @State private var month = Date.now
    @State private var view = MonarchCalendarToolbarView.month
    @State private var selection: Date? = Calendar.current.startOfDay(for: .now)
    @State private var events: [MonarchAgendaEvent] = MonarchCalendarMonthViewSection.sampleEvents

    private static var sampleEvents: [MonarchAgendaEvent] {
        let calendar = Calendar.current
        let today = calendar.startOfDay(for: .now)
        func at(_ days: Int, _ hour: Int) -> Date {
            calendar.date(byAdding: DateComponents(day: days, hour: hour), to: today) ?? today
        }
        return [
            MonarchAgendaEvent("Stand-up", id: "1", start: at(0, 9), end: at(0, 9).addingTimeInterval(900)),
            MonarchAgendaEvent("Design review", id: "2", start: at(0, 14), end: at(0, 15), status: .tentative),
            MonarchAgendaEvent("Lunch with Priya", id: "3", start: at(0, 12), end: at(0, 13)),
            MonarchAgendaEvent("Planning", id: "4", start: at(0, 16), end: at(0, 17)),
            MonarchAgendaEvent("Company offsite", id: "5", start: at(3, 0), end: at(5, 0), isAllDay: true),
            MonarchAgendaEvent("Dentist", id: "6", start: at(-2, 10), end: at(-2, 11)),
        ]
    }

    var body: some View {
        MonarchCalendarToolbar(
            month.formatted(.dateTime.month(.wide).year()),
            view: $view,
            views: [.month],
            onToday: { month = .now },
            onPrevious: { month = calendar.date(byAdding: .month, value: -1, to: month) ?? month },
            onNext: { month = calendar.date(byAdding: .month, value: 1, to: month) ?? month }
        )

        CatalogueCaption("On a phone: dots. Touch a day to list its events; swipe for other months.")
        MonarchCalendarMonthView(month: $month, events: events, selection: $selection)
        if let selection {
            MonarchAgendaList(events, from: selection, days: 1, showsEmptyDays: true)
        }

        CatalogueCaption("Chips with “+N more”. Long-press a chip and drag it to another day.")
        MonarchCalendarMonthView(
            month: $month,
            events: events,
            maxEvents: 2,
            layout: .events,
            dayState: { day in
                calendar.isDateInWeekend(day) ? MonarchCalendarMonthViewDayState(isUnavailable: true) : nil
            },
            onMoveEvent: { event, day in
                guard let index = events.firstIndex(where: { $0.id == event.id }) else { return }
                let offset =
                    calendar.dateComponents([.day], from: calendar.startOfDay(for: event.start), to: day).day ?? 0
                events[index].start = calendar.date(byAdding: .day, value: offset, to: event.start) ?? event.start
                events[index].end = event.end.flatMap { calendar.date(byAdding: .day, value: offset, to: $0) }
            },
            onSelectEvent: { _ in }
        )
    }
}
