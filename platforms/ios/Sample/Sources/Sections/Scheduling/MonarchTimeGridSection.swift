import MonarchUI
import SwiftUI

struct MonarchTimeGridSection: CataloguePage {
    static let title = "Time grid"

    @Environment(\.monarchTheme) private var theme
    @State private var view = MonarchCalendarToolbarView.week
    @State private var offset = 0
    @State private var events = MonarchTimeGridSection.sampleEvents
    @State private var last = "Tap a slot to create, tap an event to open, hold an event to move it."

    private static var calendar: Calendar { .current }

    private static func at(_ day: Int, _ hour: Int, _ minute: Int = 0) -> Date {
        let today = calendar.startOfDay(for: Date())
        let date = calendar.date(byAdding: .day, value: day, to: today) ?? today
        return calendar.date(bySettingHour: hour, minute: minute, second: 0, of: date) ?? date
    }

    private static var sampleEvents: [MonarchAgendaEvent] {
        [
            MonarchAgendaEvent("Stand-up", id: "standup", start: at(0, 9), end: at(0, 9, 15)),
            MonarchAgendaEvent("Design review", id: "review", start: at(0, 10), end: at(0, 11, 30), detail: "Room 4"),
            MonarchAgendaEvent(
                "Interview",
                id: "interview",
                start: at(0, 10, 30),
                end: at(0, 11, 30),
                status: .tentative
            ),
            MonarchAgendaEvent("Planning", id: "planning", start: at(1, 14), end: at(1, 15, 30)),
            MonarchAgendaEvent("Retrospective", id: "retro", start: at(2, 15), end: at(2, 16), status: .cancelled),
            MonarchAgendaEvent("Conference", id: "conf", start: at(1, 0), end: at(2, 0), isAllDay: true),
        ]
    }

    private var days: [Date] {
        let calendar = Self.calendar
        let today = calendar.startOfDay(for: Date())
        switch view {
        case .day:
            return [calendar.date(byAdding: .day, value: offset, to: today) ?? today]
        default:
            let start = calendar.dateInterval(of: .weekOfYear, for: today)?.start ?? today
            let first = calendar.date(byAdding: .weekOfYear, value: offset, to: start) ?? start
            return (0..<7).compactMap { calendar.date(byAdding: .day, value: $0, to: first) }
        }
    }

    var body: some View {
        MonarchCalendarToolbar(
            view == .day
                ? (days.first ?? Date()).formatted(.dateTime.weekday(.wide).day().month(.wide))
                : (days.first ?? Date()).formatted(.dateTime.day().month(.abbreviated).year()),
            view: $view,
            views: [.day, .week],
            isTodayDisabled: offset == 0,
            onToday: { offset = 0 },
            onPrevious: { offset -= 1 },
            onNext: { offset += 1 }
        )
        Text(last)
            .monarchTextStyle(.caption)
            .foregroundStyle(theme.colors.textMuted)
        TimelineView(.everyMinute) { timeline in
            MonarchTimeGrid(
                days: days,
                events: events,
                now: timeline.date,
                workingHours: (1...5).map { MonarchTimeGridWorkingHours(weekday: $0, start: 9 * 60, end: 18 * 60) },
                unavailable: [
                    MonarchTimeGridUnavailable(id: "lunch", start: Self.at(2, 12), end: Self.at(2, 13), label: "Closed")
                ],
                onSelectDay: { day in
                    view = .day
                    offset =
                        Self.calendar.dateComponents([.day], from: Self.calendar.startOfDay(for: Date()), to: day).day
                        ?? 0
                },
                onSelectSlot: { slot in
                    let id = "new-\(events.count)"
                    events.append(
                        MonarchAgendaEvent(
                            "New event",
                            id: id,
                            start: slot.start,
                            end: slot.start.addingTimeInterval(3_600)
                        )
                    )
                    last = "Created at \(slot.start.formatted(date: .omitted, time: .shortened))"
                },
                onSelectEvent: { event in last = "Opened \(event.title)" },
                onMoveEvent: { move in
                    guard let index = events.firstIndex(where: { $0.id == move.event.id }) else { return }
                    events[index].start = move.start
                    events[index].end = move.end
                    last = "Moved \(move.event.title) to \(move.start.formatted(date: .abbreviated, time: .shortened))"
                }
            )
        }
        .frame(height: theme.spacing.space24 * 6)

        CatalogueCaption("Day view, 8:00 to 18:00, read only")
        MonarchTimeGrid(days: [Date()], events: events, startHour: 8, endHour: 18)
            .frame(height: theme.spacing.space24 * 4)
    }
}
