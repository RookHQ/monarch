import SwiftUI

/// A month of days with their events: the weekday header, today and the
/// days of other months marked, event chips with a "+N more" list, or dots
/// on narrow screens, and a horizontal swipe between months.
///
/// ```swift
/// MonarchCalendarMonthView(month: $month, events: events, selection: $day) { event in
///     open(event)
/// }
/// ```
///
/// On an iPhone the days are too narrow for titles, so the automatic layout
/// shows up to three dots and touching a day selects it: list the selected
/// day's events under the grid with ``MonarchAgendaList``, as the system
/// Calendar does. On wider screens each day lists its events as
/// ``MonarchEventChip``s; with `onMoveEvent` they can be dragged to another
/// day. The first weekday, the names and the numerals come from the
/// environment's calendar and locale. Put ``MonarchCalendarToolbar`` above
/// it for the title, Today and the previous and next buttons; they and the
/// swipe both change `month`.
///
/// Accessibility: each day is read as "Wednesday, November 4, 2026, 2
/// events, today, Holiday" and has the selected trait when selected. In the
/// dots layout a day is one button; in the events layout the date, each
/// chip and "+N more" are separate buttons. Months are pages that VoiceOver
/// scrolls with a three-finger swipe. Dragging an event has no VoiceOver
/// equivalent here: offer moving in the app's event editor.
public struct MonarchCalendarMonthView: View {
    @Binding private var month: Date
    private let events: [MonarchAgendaEvent]
    private let selection: Binding<Date?>?
    private let today: Date?
    private let isFixedWeeks: Bool
    private let maxEvents: Int
    private let layout: MonarchCalendarMonthViewLayout
    private let labels: MonarchCalendarMonthViewLabels
    private let dayState: (@MainActor (Date) -> MonarchCalendarMonthViewDayState?)?
    private let onSelectDay: (@MainActor (Date) -> Void)?
    private let onMoveEvent: (@MainActor (MonarchAgendaEvent, Date) -> Void)?
    private let onSelectEvent: (@MainActor (MonarchAgendaEvent) -> Void)?

    @Environment(\.calendar) private var calendar
    @Environment(\.monarchTheme) private var theme
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var anchor: Date?
    @State private var position: Date?

    /// A month calendar with events.
    ///
    /// - Parameters:
    ///   - month: Any date in the month on screen. A swipe writes the first day of the month
    ///     the person moves to.
    ///   - events: The events; each appears on every day it touches.
    ///   - selection: The selected day. Touching a day sets it to the first moment of that day.
    ///   - today: Today, for previews and tests. Without it the clock is read.
    ///   - isFixedWeeks: Whether every month shows six weeks, so the height does not change.
    ///   - maxEvents: How many rows a day uses for events in the events layout, the "+N more"
    ///     button included.
    ///   - layout: Chips, dots, or chips only when the days are wide enough.
    ///   - labels: Replacements for the built-in strings.
    ///   - dayState: Shading and a badge for a day, or `nil`. Called for every shown day.
    ///   - onSelectDay: Runs when a day is touched, after `selection` is set.
    ///   - onMoveEvent: Allows dragging chips to another day; receives the event and the first
    ///     moment of the day it was dropped on. Keep the time of day when you save it.
    ///   - onSelectEvent: Opens an event when its chip is touched.
    public init(
        month: Binding<Date>,
        events: [MonarchAgendaEvent] = [],
        selection: Binding<Date?>? = nil,
        today: Date? = nil,
        isFixedWeeks: Bool = true,
        maxEvents: Int = 3,
        layout: MonarchCalendarMonthViewLayout = .automatic,
        labels: MonarchCalendarMonthViewLabels = MonarchCalendarMonthViewLabels(),
        dayState: (@MainActor (Date) -> MonarchCalendarMonthViewDayState?)? = nil,
        onSelectDay: (@MainActor (Date) -> Void)? = nil,
        onMoveEvent: (@MainActor (MonarchAgendaEvent, Date) -> Void)? = nil,
        onSelectEvent: (@MainActor (MonarchAgendaEvent) -> Void)? = nil
    ) {
        _month = month
        self.events = events
        self.selection = selection
        self.today = today
        self.isFixedWeeks = isFixedWeeks
        self.maxEvents = maxEvents
        self.layout = layout
        self.labels = labels
        self.dayState = dayState
        self.onSelectDay = onSelectDay
        self.onMoveEvent = onMoveEvent
        self.onSelectEvent = onSelectEvent
    }

    private var current: Date { MonarchCalendarMonthViewTokens.monthStart(month, calendar: calendar) }

    private var context: MonarchCalendarMonthViewContext {
        MonarchCalendarMonthViewContext(
            events: events,
            selection: selection?.wrappedValue,
            today: today ?? .now,
            isFixedWeeks: isFixedWeeks,
            maxEvents: maxEvents,
            layout: layout,
            labels: labels,
            dayState: dayState,
            onSelectDay: { day in
                selection?.wrappedValue = calendar.startOfDay(for: day)
                onSelectDay?(calendar.startOfDay(for: day))
            },
            onSelectEvent: onSelectEvent,
            onMoveEvent: onMoveEvent
        )
    }

    /// The content of the view.
    public var body: some View {
        let months = MonarchCalendarMonthViewTokens.months(
            around: anchor ?? current,
            radius: MonarchCalendarMonthViewTokens.pagerRadius,
            calendar: calendar
        )
        let context = context
        ScrollViewReader { proxy in
            ScrollView(.horizontal) {
                LazyHStack(alignment: .top, spacing: 0) {
                    ForEach(months, id: \.self) { page in
                        MonarchCalendarMonthViewGrid(month: page, context: context)
                            .containerRelativeFrame(.horizontal)
                            .id(page)
                    }
                }
                .scrollTargetLayout()
            }
            .scrollTargetBehavior(.paging)
            .scrollIndicators(.hidden)
            .scrollPosition(id: $position)
            .onAppear {
                if anchor == nil { anchor = current }
                position = current
                proxy.scrollTo(current)
            }
            .onChange(of: position) { _, page in
                guard let page, page != current else { return }
                month = page
            }
            .onChange(of: current) { _, target in
                guard position != target else { return }
                if !months.contains(target) { anchor = target }
                withAnimation(theme.motion.animation(reduceMotion: reduceMotion)) {
                    position = target
                }
            }
        }
    }
}

#if DEBUG
    #Preview("MonarchCalendarMonthView") {
        let today = Date(timeIntervalSince1970: 1_794_614_400)
        MonarchPreviewMatrix {
            MonarchCalendarMonthView(
                month: .constant(today),
                events: [
                    MonarchAgendaEvent("Stand-up", id: "1", start: today.addingTimeInterval(9 * 3_600)),
                    MonarchAgendaEvent(
                        "Review",
                        id: "2",
                        start: today.addingTimeInterval(14 * 3_600),
                        status: .tentative
                    ),
                    MonarchAgendaEvent(
                        "Offsite",
                        id: "3",
                        start: today,
                        end: today.addingTimeInterval(2 * 86_400),
                        isAllDay: true
                    ),
                ],
                selection: .constant(today),
                today: today
            )
        }
    }
#endif
