import MonarchTokens
import SwiftUI

/// Events grouped by day, in order: a heading for each day with a count and
/// a Today marker, all-day events first, and an empty state when nothing is
/// scheduled.
///
/// ```swift
/// MonarchAgendaList(events) { event in
///     selected = event
/// }
/// ```
///
/// The agenda is a plain `List` with a `Section` per day, so it scrolls,
/// keeps day headings pinned while scrolling, and fills the space it is
/// given: use it as the content of a screen or a tab, not inside another
/// scroll view. Events are grouped in the calendar's time zone; an event that
/// spans several days appears on each, with "All day" on the days it covers
/// whole and only its end time on its last day.
///
/// Accessibility: each day heading is one element with the header trait,
/// read as "Wednesday 4 November, Today, 3 events", so the headings rotor
/// moves between days. Each event is a ``MonarchEventChip`` read as "title,
/// time, status, detail", with the time spoken in full ("9:30 to 10:15").
/// With `onSelect` the events are buttons with a 44-point touch target. The
/// Today marker is a word, not only a colour. Text scales with Dynamic Type.
public struct MonarchAgendaList: View {
    private let events: [MonarchAgendaEvent]
    private let from: Date?
    private let days: Int?
    private let today: Date?
    private let showsEmptyDays: Bool
    private let calendar: Calendar
    private let labels: MonarchAgendaListLabels
    private let dayNotes: [Date: [String]]
    private let onSelect: (@MainActor (MonarchAgendaEvent) -> Void)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale

    /// An agenda.
    ///
    /// - Parameters:
    ///   - events: The events, in any order.
    ///   - from: The first day listed. The default is the first day with an event. Events that
    ///     started earlier and are still running appear on it.
    ///   - days: How many days from `from` to cover. The default runs through the last event.
    ///   - today: The day marked Today, or `nil` for no marker. The default is now.
    ///   - showsEmptyDays: Whether days in the range with no events are listed.
    ///   - calendar: The calendar and time zone events are grouped in.
    ///   - labels: Replacements for the built-in strings.
    ///   - dayNotes: Informational lines shown first in a day, such as time off or a holiday,
    ///     keyed by any time on that day. A day with notes is listed even when it has no events.
    ///   - onSelect: Opens an event. Without it the events are not buttons.
    public init(
        _ events: [MonarchAgendaEvent],
        from: Date? = nil,
        days: Int? = nil,
        today: Date? = Date(),
        showsEmptyDays: Bool = false,
        calendar: Calendar = .autoupdatingCurrent,
        labels: MonarchAgendaListLabels = MonarchAgendaListLabels(),
        dayNotes: [Date: [String]] = [:],
        onSelect: (@MainActor (MonarchAgendaEvent) -> Void)? = nil
    ) {
        self.events = events
        self.from = from
        self.days = days
        self.today = today
        self.showsEmptyDays = showsEmptyDays
        self.calendar = calendar
        self.labels = labels
        self.dayNotes = dayNotes
        self.onSelect = onSelect
    }

    /// The content of the view.
    public var body: some View {
        let agenda = MonarchAgendaListTokens.days(
            events: events,
            from: from,
            days: days,
            showsEmptyDays: showsEmptyDays,
            calendar: calendar,
            notes: { day in MonarchAgendaListTokens.notes(on: day, from: dayNotes, calendar: calendar) }
        )
        if agenda.isEmpty {
            MonarchEmptyState(
                MonarchAgendaListTokens.emptyTitle(labels),
                systemImage: MonarchAgendaListTokens.emptySymbol
            )
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else {
            List {
                ForEach(agenda) { day in
                    Section {
                        ForEach(day.notes, id: \.self) { note in
                            noteRow(note)
                        }
                        ForEach(day.events) { event in
                            eventRow(event, on: day.day)
                        }
                    } header: {
                        heading(day)
                    }
                }
            }
            .listStyle(.plain)
            .scrollContentBackground(.hidden)
        }
    }

    private func heading(_ day: MonarchAgendaDay) -> some View {
        let isToday = today.map { calendar.isDate($0, inSameDayAs: day.day) } ?? false
        let date = day.day.formatted(dateFormat)
        let count = MonarchAgendaListTokens.count(day.events.count, labels: labels, locale: locale)
        return HStack(alignment: .firstTextBaseline, spacing: MonarchAgendaListTokens.headingSpacing) {
            Text(date)
                .monarchTextStyle(MonarchAgendaListTokens.headingStyle)
                .fontWeight(MonarchAgendaListTokens.headingWeight)
                // Today's heading is in the accent colour, as on the web.
                .foregroundStyle(
                    isToday ? theme.colors.accentStrong : theme.colors[role: MonarchAgendaListTokens.headingColor]
                )
            if isToday {
                MonarchBadge(labels.today ?? MonarchAgendaListStrings.today, tone: MonarchAgendaListTokens.todayTone)
                    .textCase(.uppercase)
            }
            Spacer(minLength: 0)
            Text(count)
                .monarchTextStyle(MonarchAgendaListTokens.captionStyle)
                .foregroundStyle(theme.colors[role: MonarchAgendaListTokens.secondaryColor])
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(
            MonarchAgendaListTokens.headingLabel(date: date, isToday: isToday, count: count, labels: labels)
        )
        .accessibilityAddTraits(.isHeader)
    }

    private func noteRow(_ note: String) -> some View {
        // A quiet tinted row, as the web draws a day note, without an icon.
        Text(note)
            .monarchTextStyle(MonarchAgendaListTokens.captionStyle)
            .foregroundStyle(theme.colors[role: MonarchAgendaListTokens.secondaryColor])
            .padding(.horizontal, MonarchTokens.Space.space3)
            .padding(.vertical, MonarchTokens.Space.space2)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                RoundedRectangle(cornerRadius: MonarchTokens.Radius.md, style: .continuous)
                    .fill(theme.colors.bgMuted)
            )
            .listRowBackground(Color.clear)
            .listRowSeparator(.hidden)
    }

    private func eventRow(_ event: MonarchAgendaEvent, on day: Date) -> some View {
        let time = MonarchAgendaListTokens.time(
            of: event,
            on: day,
            calendar: calendar,
            labels: labels,
            format: { $0.formatted(timeFormat) }
        )
        var open: (@MainActor () -> Void)?
        if let onSelect {
            open = { onSelect(event) }
        }
        return MonarchEventChip(
            event.title,
            time: time.text,
            timeLabel: time.spoken,
            detail: event.detail,
            color: event.color,
            status: event.status,
            variant: .bar,
            action: open
        )
        .padding(.vertical, MonarchAgendaListTokens.rowPadding)
        .listRowBackground(Color.clear)
        .listRowSeparator(.hidden)
    }

    private var dateFormat: Date.FormatStyle {
        Date.FormatStyle(
            date: .omitted,
            time: .omitted,
            locale: locale,
            calendar: calendar,
            timeZone: calendar.timeZone
        )
        .weekday(.wide).day().month(.wide)
    }

    private var timeFormat: Date.FormatStyle {
        Date.FormatStyle(
            date: .omitted,
            time: .omitted,
            locale: locale,
            calendar: calendar,
            timeZone: calendar.timeZone
        ).monarchTime()
    }
}

#if DEBUG
    #Preview("MonarchAgendaList") {
        let start = Calendar.current.startOfDay(for: Date())
        MonarchPreviewMatrix {
            MonarchAgendaList(
                [
                    MonarchAgendaEvent(
                        "Stand-up",
                        id: "1",
                        start: start.addingTimeInterval(9.5 * 3600),
                        end: start.addingTimeInterval(9.75 * 3600)
                    ),
                    MonarchAgendaEvent("Offsite", id: "2", start: start, isAllDay: true, status: .tentative),
                    MonarchAgendaEvent(
                        "Design review",
                        id: "3",
                        start: start.addingTimeInterval(30 * 3600),
                        end: start.addingTimeInterval(31 * 3600),
                        detail: "Room 4"
                    ),
                ],
                dayNotes: [Date(): ["Ada is off"]]
            ) { _ in }
            .frame(height: MonarchTokens.Space.space24 * 4)
        }
    }
#endif
