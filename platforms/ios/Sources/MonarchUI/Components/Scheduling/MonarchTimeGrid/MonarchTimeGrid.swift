import MonarchTokens
import SwiftUI

/// The day and week view of a calendar: an hour gutter, an all-day row and
/// one column per day, with overlapping events side by side, a now line,
/// working-hours shading, unavailable blocks, tap to create and long press
/// and drag to move.
///
/// ```swift
/// MonarchTimeGrid(
///     days: week,
///     events: events,
///     onSelectSlot: { slot in create(at: slot.start) },
///     onSelectEvent: { event in selected = event },
///     onMoveEvent: { move in reschedule(move) }
/// )
/// ```
///
/// The heading stays in place and the hours scroll, starting an hour before
/// the earliest event (or at `scrollToMinutes`). Events are laid out in the
/// calendar's time zone by wall-clock time, so a daylight-saving day still
/// shows 24 hour rows. Events are ``MonarchAgendaEvent`` values, the same
/// model as the agenda list, drawn as ``MonarchEventChip`` blocks and bars.
///
/// Moving follows Calendar: press and hold an event, then drag it to another
/// time or day. Scrolling is paused while an event is held, a haptic marks
/// the pick-up and every step, and the event snaps to `snapMinutes`. The app
/// receives a ``MonarchTimeGridMove`` and decides whether to apply it.
///
/// The now line does not move by itself: pass `now` from a
/// `TimelineView(.everyMinute)` to keep it current, or `nil` to hide it.
///
/// Accessibility: each day heading is read in full ("Wednesday 4 November,
/// Today"). Each event is one element read as "title, time, status, detail",
/// a button when `onSelectEvent` is set, and, when it can move, has the
/// actions Move earlier, Move later, Move to previous day and Move to next
/// day, so it can be rescheduled without dragging. With `onSelectSlot`, each
/// empty hour is a button read as "Create event, Wednesday 4 November,
/// 09:00". Today is a word as well as a filled circle; unavailable blocks are
/// read with their label and times. Text and hour height scale with Dynamic
/// Type.
public struct MonarchTimeGrid: View {
    private let days: [Date]
    private let events: [MonarchAgendaEvent]
    private let now: Date?
    private let startHour: Int
    private let endHour: Int
    private let hourHeight: CGFloat?
    private let snapMinutes: Int
    private let workingHours: [MonarchTimeGridWorkingHours]?
    private let unavailable: [MonarchTimeGridUnavailable]
    private let scrollToMinutes: Int?
    private let showsAllDayRow: Bool
    private let calendar: Calendar
    private let labels: MonarchTimeGridLabels
    private let canMoveEvent: (@MainActor (MonarchAgendaEvent) -> Bool)?
    private let onSelectDay: (@MainActor (Date) -> Void)?
    private let onSelectSlot: (@MainActor (MonarchTimeGridSlot) -> Void)?
    private let onSelectEvent: (@MainActor (MonarchAgendaEvent) -> Void)?
    private let onMoveEvent: (@MainActor (MonarchTimeGridMove) -> Void)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    @ScaledMetric(relativeTo: .caption) private var scaledHourHeight = MonarchTimeGridTokens.hourHeight
    @ScaledMetric(relativeTo: .caption) private var gutterMinWidth = MonarchTimeGridTokens.gutterMinWidth
    @ScaledMetric(relativeTo: .caption) private var eventMinHeight = MonarchTimeGridTokens.eventMinHeight
    @State private var gutterWidth: CGFloat = 0
    @State private var viewWidth: CGFloat = 0
    @State private var scrollX: CGFloat = 0
    @State private var columnFrames: [Int: CGRect] = [:]
    @GestureState private var drag: MonarchTimeGridDrag?

    /// A day or week grid.
    ///
    /// - Parameters:
    ///   - days: The day columns, in order: one for a day view, seven for a
    ///     week. Any time on each day.
    ///   - events: The events. Timed events are placed by their start and
    ///     end; all-day events go in the all-day row of each day they cover.
    ///   - now: The current time, for the now line and today's heading, or
    ///     `nil` for neither.
    ///   - startHour: The first visible hour, 0 to 23.
    ///   - endHour: The end of the visible hours (exclusive), 1 to 24.
    ///   - hourHeight: The height of one hour. The default is 48 points,
    ///     scaled with Dynamic Type.
    ///   - snapMinutes: Taps and moves snap to this many minutes.
    ///   - workingHours: The working hours; time outside them is shaded.
    ///     `nil` shades nothing.
    ///   - unavailable: Blocks drawn hatched, such as time off.
    ///   - scrollToMinutes: Minutes from midnight to scroll to when the grid
    ///     appears and when the first day changes. The default is an hour
    ///     before the earliest event, else 08:00.
    ///   - showsAllDayRow: Whether the all-day row shows when no all-day
    ///     event is visible.
    ///   - calendar: The calendar and time zone the grid is drawn in.
    ///   - labels: Replacements for the built-in strings.
    ///   - canMoveEvent: Which events may be moved. The default is all,
    ///     when `onMoveEvent` is set.
    ///   - onSelectDay: Makes the headings of a multi-day grid buttons, for
    ///     example to open the day view. Receives the day.
    ///   - onSelectSlot: Called when an empty slot is tapped, with the time
    ///     snapped to `snapMinutes`. Turns on the slot buttons for VoiceOver.
    ///   - onSelectEvent: Opens an event. Without it events are not buttons.
    ///   - onMoveEvent: Turns on moving events, by long press and drag and by
    ///     VoiceOver actions.
    public init(
        days: [Date],
        events: [MonarchAgendaEvent] = [],
        now: Date? = Date(),
        startHour: Int = 0,
        endHour: Int = 24,
        hourHeight: CGFloat? = nil,
        snapMinutes: Int = 15,
        workingHours: [MonarchTimeGridWorkingHours]? = nil,
        unavailable: [MonarchTimeGridUnavailable] = [],
        scrollToMinutes: Int? = nil,
        showsAllDayRow: Bool = false,
        calendar: Calendar = .autoupdatingCurrent,
        labels: MonarchTimeGridLabels = MonarchTimeGridLabels(),
        canMoveEvent: (@MainActor (MonarchAgendaEvent) -> Bool)? = nil,
        onSelectDay: (@MainActor (Date) -> Void)? = nil,
        onSelectSlot: (@MainActor (MonarchTimeGridSlot) -> Void)? = nil,
        onSelectEvent: (@MainActor (MonarchAgendaEvent) -> Void)? = nil,
        onMoveEvent: (@MainActor (MonarchTimeGridMove) -> Void)? = nil
    ) {
        self.days = days
        self.events = events
        self.now = now
        self.startHour = startHour
        self.endHour = endHour
        self.hourHeight = hourHeight
        self.snapMinutes = max(1, snapMinutes)
        self.workingHours = workingHours
        self.unavailable = unavailable
        self.scrollToMinutes = scrollToMinutes
        self.showsAllDayRow = showsAllDayRow
        self.calendar = calendar
        self.labels = labels
        self.canMoveEvent = canMoveEvent
        self.onSelectDay = onSelectDay
        self.onSelectSlot = onSelectSlot
        self.onSelectEvent = onSelectEvent
        self.onMoveEvent = onMoveEvent
    }

    /// The content of the view.
    public var body: some View {
        let range = MonarchTimeGridModel.range(startHour: startHour, endHour: endHour)
        let columns = MonarchTimeGridModel.columns(
            days: days,
            events: events,
            unavailable: unavailable,
            workingHours: workingHours,
            range: range,
            snapMinutes: snapMinutes,
            calendar: calendar
        )
        let context = MonarchTimeGridContext(
            range: range,
            minuteHeight: (hourHeight ?? scaledHourHeight) / 60,
            formats: MonarchTimeGridTokens.Formats(calendar: calendar, locale: locale)
        )
        let target = MonarchTimeGridModel.scrollTarget(requested: scrollToMinutes, columns: columns, range: range)
        let daysWidth = dayColumnsWidth(count: columns.count)
        ScrollView(.horizontal, showsIndicators: false) {
            VStack(spacing: 0) {
                heading(columns, context: context, daysWidth: daysWidth)
                ScrollViewReader { proxy in
                    ScrollView(.vertical) {
                        grid(columns, context: context, daysWidth: daysWidth)
                    }
                    .scrollDisabled(drag != nil)
                    .onAppear { proxy.scrollTo(target / 60, anchor: .top) }
                    .onChange(of: scrollKey) { proxy.scrollTo(target / 60, anchor: .top) }
                }
            }
            .frame(width: daysWidth.map { $0 + gutterWidth }, alignment: .leading)
            .background(alignment: .leading) {
                // How far the days have scrolled: the hours stay where they are by moving back by it.
                Color.clear.frame(width: 0)
                    .onGeometryChange(
                        for: CGFloat.self,
                        of: { $0.frame(in: .named(Self.scrollSpace)).minX },
                        action: { scrollX = max(0, -$0) }
                    )
            }
        }
        .coordinateSpace(.named(Self.scrollSpace))
        .scrollBounceBehavior(.basedOnSize, axes: .horizontal)
        .onGeometryChange(for: CGFloat.self, of: { $0.size.width }, action: { viewWidth = $0 })
        .background(theme.colors[role: MonarchTimeGridTokens.background])
        .sensoryFeedback(trigger: drag) { old, new in
            guard let new else { return nil }
            guard let old else { return .impact }
            return old.dayIndex != new.dayIndex || old.startMinutes != new.startMinutes ? .selection : nil
        }
        .accessibilityElement(children: .contain)
    }

    private var scrollKey: String {
        "\(days.first?.timeIntervalSinceReferenceDate ?? 0):\(days.count)"
    }

    private nonisolated static let scrollSpace = "monarch.timeGrid.scroll"

    /// The width of all the day columns together: the room beside the hours, or `count` columns of the least
    /// width when that is more, so that a narrow screen scrolls sideways and event names are not squeezed.
    private func dayColumnsWidth(count: Int) -> CGFloat? {
        guard viewWidth > 0, gutterWidth > 0, count > 0 else { return nil }
        return max(viewWidth - gutterWidth, CGFloat(count) * MonarchTimeGridTokens.minDayWidth)
    }

    private var isMultiDay: Bool { days.count > 1 }

    // MARK: Heading

    private func heading(
        _ columns: [MonarchTimeGridColumnModel],
        context: MonarchTimeGridContext,
        daysWidth: CGFloat?
    ) -> some View {
        let hasAllDay = showsAllDayRow || columns.contains { !$0.allDay.isEmpty }
        let allDay = MonarchTimeGridTokens.allDay(labels)
        return VStack(spacing: 0) {
            HStack(spacing: 0) {
                Color.clear.frame(width: gutterWidth, height: 0)
                HStack(spacing: 0) {
                    ForEach(columns.indices, id: \.self) { index in
                        MonarchTimeGridDayHeading(
                            day: columns[index].day,
                            isToday: isToday(columns[index].day),
                            formats: context.formats,
                            labels: labels,
                            action: dayAction(columns[index].day)
                        )
                        .frame(maxWidth: .infinity)
                    }
                }
                .frame(width: daysWidth)
            }
            if hasAllDay {
                HStack(alignment: .top, spacing: 0) {
                    Text(allDay)
                        .monarchTextStyle(MonarchTimeGridTokens.captionStyle)
                        .foregroundStyle(theme.colors[role: MonarchTimeGridTokens.secondaryText])
                        .multilineTextAlignment(.trailing)
                        .padding(.horizontal, MonarchTimeGridTokens.gutterPadding)
                        .frame(width: gutterWidth, alignment: .trailing)
                        .background(theme.colors[role: MonarchTimeGridTokens.background])
                        .offset(x: scrollX)
                        .zIndex(1)
                        .accessibilityHidden(true)
                    HStack(alignment: .top, spacing: 0) {
                        ForEach(columns.indices, id: \.self) { index in
                            allDayCell(columns[index], allDay: allDay, formats: context.formats)
                        }
                    }
                    .frame(width: daysWidth)
                }
                .padding(.vertical, MonarchTimeGridTokens.headingSpacing)
            }
            Rectangle()
                .fill(theme.colors[role: MonarchTimeGridTokens.headingLine])
                .frame(height: MonarchTimeGridTokens.lineWidth)
                .accessibilityHidden(true)
        }
    }

    private func allDayCell(
        _ column: MonarchTimeGridColumnModel,
        allDay: String,
        formats: MonarchTimeGridTokens.Formats
    ) -> some View {
        VStack(spacing: MonarchTimeGridTokens.headingSpacing) {
            ForEach(column.allDay) { event in
                MonarchEventChip(
                    event.title,
                    timeLabel: allDay,
                    color: event.color,
                    status: event.status,
                    variant: .bar,
                    action: selectAction(event)
                )
            }
        }
        .padding(.horizontal, MonarchTimeGridTokens.eventGap)
        .frame(maxWidth: .infinity, alignment: .top)
        .accessibilityElement(children: .contain)
        .accessibilityLabel("\(formats.longDay(column.day)), \(allDay)")
    }

    // MARK: Grid

    private func grid(
        _ columns: [MonarchTimeGridColumnModel],
        context: MonarchTimeGridContext,
        daysWidth: CGFloat?
    ) -> some View {
        let hours = MonarchTimeGridModel.hours(in: context.range)
        let hourHeight = context.minuteHeight * 60
        return HStack(alignment: .top, spacing: 0) {
            VStack(alignment: .trailing, spacing: 0) {
                ForEach(hours, id: \.self) { hour in
                    Text(hour == hours.first ? "" : context.formats.minutes(hour * 60))
                        .monarchTextStyle(MonarchTimeGridTokens.captionStyle)
                        .monospacedDigit()
                        .lineLimit(1)
                        .fixedSize()
                        .foregroundStyle(theme.colors[role: MonarchTimeGridTokens.secondaryText])
                        // The label is centred on its hour line.
                        .alignmentGuide(.top) { $0[VerticalAlignment.center] }
                        .frame(height: hourHeight, alignment: .top)
                        .id(hour)
                }
            }
            .padding(.horizontal, MonarchTimeGridTokens.gutterPadding)
            .frame(minWidth: gutterMinWidth, alignment: .trailing)
            .fixedSize(horizontal: true, vertical: false)
            .onGeometryChange(for: CGFloat.self, of: { $0.size.width }, action: { gutterWidth = $0 })
            .background(theme.colors[role: MonarchTimeGridTokens.background])
            .offset(x: scrollX)
            .zIndex(1)
            .accessibilityHidden(true)

            HStack(spacing: 0) {
                ForEach(columns.indices, id: \.self) { index in
                    column(columns[index], index: index, hours: hours, context: context)
                }
            }
            .frame(width: daysWidth)
            .background(alignment: .top) {
                VStack(spacing: 0) {
                    ForEach(hours, id: \.self) { _ in
                        Rectangle()
                            .fill(theme.colors[role: MonarchTimeGridTokens.line])
                            .frame(height: MonarchTimeGridTokens.lineWidth)
                            .frame(height: hourHeight, alignment: .top)
                    }
                }
                .accessibilityHidden(true)
            }
        }
    }

    private func column(
        _ column: MonarchTimeGridColumnModel,
        index: Int,
        hours: [Int],
        context: MonarchTimeGridContext
    ) -> some View {
        let longDay = context.formats.longDay(column.day)
        return MonarchTimeGridColumnLayout(
            range: context.range,
            minuteHeight: context.minuteHeight,
            gap: MonarchTimeGridTokens.eventGap,
            trailingInset: MonarchTimeGridTokens.columnTrailingInset,
            eventMinHeight: eventMinHeight
        ) {
            ForEach(column.shade, id: \.self) { span in
                Rectangle()
                    .fill(theme.colors[role: MonarchTimeGridTokens.shade])
                    .allowsHitTesting(false)
                    .accessibilityHidden(true)
                    .monarchTimeGridPlacement(MonarchTimeGridPlacement(span: span))
            }
            ForEach(column.unavailable, id: \.item.id) { block in
                MonarchTimeGridUnavailableBlock(
                    label: block.item.label,
                    spokenLabel: MonarchTimeGridTokens.unavailableLabel(
                        block.item.label,
                        time:
                            "\(context.formats.minutes(block.span.top)) – \(context.formats.minutes(block.span.bottom))"
                    )
                )
                .monarchTimeGridPlacement(MonarchTimeGridPlacement(span: block.span))
            }
            if let onSelectSlot {
                ForEach(hours, id: \.self) { hour in
                    Color.clear
                        .contentShape(Rectangle())
                        .allowsHitTesting(false)
                        .accessibilityElement()
                        .accessibilityLabel(
                            MonarchTimeGridTokens.slotLabel(
                                longDay: longDay,
                                time: context.formats.minutes(hour * 60),
                                labels: labels
                            )
                        )
                        .accessibilityAddTraits(.isButton)
                        .accessibilityAction { onSelectSlot(slot(column.day, minutes: hour * 60)) }
                        .monarchTimeGridPlacement(
                            MonarchTimeGridPlacement(span: MonarchTimeGridSpan(top: hour * 60, bottom: hour * 60 + 60))
                        )
                }
            }
            ForEach(column.timed, id: \.item.id) { packed in
                eventView(packed, column: column, index: index, context: context)
                    .monarchTimeGridPlacement(
                        MonarchTimeGridPlacement(
                            span: packed.span,
                            column: packed.column,
                            columns: packed.columns,
                            isEvent: true
                        )
                    )
            }
            if let drag, drag.dayIndex == index,
                let event = events.first(where: { $0.id == drag.eventID })
            {
                MonarchTimeGridGhost(
                    text: "\(context.formats.minutes(drag.startMinutes)) · \(event.title)",
                    color: event.color
                )
                .monarchTimeGridPlacement(
                    MonarchTimeGridPlacement(
                        span: MonarchTimeGridSpan(
                            top: drag.startMinutes,
                            bottom: min(context.range.bottom, drag.startMinutes + drag.duration)
                        ),
                        isEvent: true
                    )
                )
            }
            if let now, isToday(column.day) {
                let minutes = MonarchTimeGridModel.wallClock(now, calendar: calendar).minutes
                if minutes >= context.range.top, minutes < context.range.bottom {
                    MonarchTimeGridNowLine()
                        .monarchTimeGridPlacement(
                            MonarchTimeGridPlacement(
                                span: MonarchTimeGridSpan(top: minutes, bottom: minutes),
                                lineHeight: MonarchTimeGridTokens.nowDotSize
                            )
                        )
                }
            }
        }
        .frame(maxWidth: .infinity)
        .background {
            if let onSelectSlot {
                Color.clear
                    .contentShape(Rectangle())
                    .onTapGesture(coordinateSpace: .local) { location in
                        let minutes = MonarchTimeGridModel.slotMinutes(
                            offset: location.y,
                            minuteHeight: context.minuteHeight,
                            range: context.range,
                            snapMinutes: snapMinutes
                        )
                        onSelectSlot(slot(column.day, minutes: minutes))
                    }
                    .accessibilityHidden(true)
            }
        }
        .overlay(alignment: .leading) {
            Rectangle()
                .fill(theme.colors[role: MonarchTimeGridTokens.line])
                .frame(width: MonarchTimeGridTokens.lineWidth)
                .allowsHitTesting(false)
                .accessibilityHidden(true)
        }
        .onGeometryChange(for: CGRect.self, of: { $0.frame(in: .global) }, action: { columnFrames[index] = $0 })
        .accessibilityElement(children: .contain)
        .accessibilityLabel(longDay)
    }

    // MARK: Events

    @ViewBuilder
    private func eventView(
        _ packed: MonarchTimeGridPacked<MonarchAgendaEvent>,
        column: MonarchTimeGridColumnModel,
        index: Int,
        context: MonarchTimeGridContext
    ) -> some View {
        let event = packed.item
        let minutes = packed.span.bottom - packed.span.top
        let isCompact = MonarchTimeGridTokens.isCompact(minutes: minutes)
        let time = MonarchTimeGridTokens.time(
            of: event,
            on: column.day,
            calendar: calendar,
            labels: labels,
            format: context.formats.time
        )
        let isMovable = canMove(event)
        let chip = MonarchEventChip(
            event.title,
            time: isCompact ? context.formats.time(max(event.start, column.day)) : time.text,
            timeLabel: time.spoken,
            detail: MonarchTimeGridTokens.showsDetail(minutes: minutes) ? event.detail : nil,
            color: event.color,
            status: event.status,
            variant: .block,
            isCompact: isCompact,
            action: isMovable ? nil : selectAction(event)
        )
        if isMovable {
            chip
                .opacity(drag?.eventID == event.id ? MonarchTimeGridTokens.movingRatio : 1)
                .contentShape(Rectangle())
                .onTapGesture { onSelectEvent?(event) }
                .gesture(moveGesture(event, top: packed.span.top, dayIndex: index, context: context))
                .accessibilityAddTraits(onSelectEvent == nil ? [] : .isButton)
                .accessibilityAction { onSelectEvent?(event) }
                .accessibilityActions {
                    ForEach(moveActions(event, context: context), id: \.name) { action in
                        Button(action.name) { onMoveEvent?(action.move) }
                    }
                }
        } else {
            chip
        }
    }

    private func moveGesture(
        _ event: MonarchAgendaEvent,
        top: Int,
        dayIndex: Int,
        context: MonarchTimeGridContext
    ) -> some Gesture {
        let frames = columnFrames
        let duration = MonarchTimeGridModel.duration(of: event, snapMinutes: snapMinutes)
        let snap = snapMinutes
        let range = context.range
        let minuteHeight = context.minuteHeight
        func state(for value: DragGesture.Value) -> MonarchTimeGridDrag? {
            guard let origin = frames[dayIndex],
                let index = MonarchTimeGridModel.columnIndex(at: value.location.x, frames: frames),
                let frame = frames[index], minuteHeight > 0
            else { return nil }
            let grabbed = Double(range.top) + Double((value.startLocation.y - origin.minY) / minuteHeight)
            let start = MonarchTimeGridDrag(
                eventID: event.id,
                grab: Int(grabbed.rounded()) - top,
                duration: duration,
                dayIndex: dayIndex,
                startMinutes: top
            )
            let minutes = Double(range.top) + Double((value.location.y - frame.minY) / minuteHeight)
            return MonarchTimeGridModel.drag(start, to: index, minutes: minutes, range: range, snapMinutes: snap)
        }
        return LongPressGesture(minimumDuration: MonarchTimeGridTokens.pressDuration)
            .sequenced(before: DragGesture(minimumDistance: 0, coordinateSpace: .global))
            .updating($drag) { value, current, _ in
                guard case .second(true, let dragValue) = value else { return }
                guard let dragValue else {
                    current = MonarchTimeGridDrag(
                        eventID: event.id,
                        grab: 0,
                        duration: duration,
                        dayIndex: dayIndex,
                        startMinutes: top
                    )
                    return
                }
                current = state(for: dragValue)
            }
            .onEnded { value in
                guard case .second(true, let dragValue?) = value, let final = state(for: dragValue),
                    days.indices.contains(final.dayIndex),
                    let move = MonarchTimeGridModel.move(
                        event,
                        to: calendar.startOfDay(for: days[final.dayIndex]),
                        startMinutes: final.startMinutes,
                        snapMinutes: snap,
                        calendar: calendar
                    )
                else { return }
                onMoveEvent?(move)
            }
    }

    private func moveActions(
        _ event: MonarchAgendaEvent,
        context: MonarchTimeGridContext
    ) -> [MonarchTimeGridMoveAction] {
        let names = MonarchTimeGridTokens.moveActionNames(labels)
        let steps: [(minutes: Int, days: Int)] = [(-snapMinutes, 0), (snapMinutes, 0), (0, -1), (0, 1)]
        return zip(names, steps).compactMap { name, step in
            MonarchTimeGridModel.nudge(
                event,
                minutes: step.minutes,
                days: step.days,
                columns: days,
                range: context.range,
                snapMinutes: snapMinutes,
                calendar: calendar
            ).map { MonarchTimeGridMoveAction(name: name, move: $0) }
        }
    }

    // MARK: Helpers

    private func canMove(_ event: MonarchAgendaEvent) -> Bool {
        onMoveEvent != nil && (canMoveEvent?(event) ?? true)
    }

    private func isToday(_ day: Date) -> Bool {
        now.map { calendar.isDate($0, inSameDayAs: day) } ?? false
    }

    private func slot(_ day: Date, minutes: Int) -> MonarchTimeGridSlot {
        MonarchTimeGridSlot(
            day: day,
            minutes: minutes,
            start: MonarchTimeGridModel.date(day: day, minutes: minutes, calendar: calendar)
        )
    }

    private func selectAction(_ event: MonarchAgendaEvent) -> (@MainActor () -> Void)? {
        guard let onSelectEvent else { return nil }
        return { onSelectEvent(event) }
    }

    private func dayAction(_ day: Date) -> (@MainActor () -> Void)? {
        guard isMultiDay, let onSelectDay else { return nil }
        return { onSelectDay(day) }
    }
}

/// What every part of the grid needs to place itself.
struct MonarchTimeGridContext {
    let range: MonarchTimeGridSpan
    let minuteHeight: CGFloat
    let formats: MonarchTimeGridTokens.Formats
}

#if DEBUG
    #Preview("MonarchTimeGrid") {
        let calendar = Calendar.current
        let today = calendar.startOfDay(for: Date())
        let week = (0..<5).compactMap { calendar.date(byAdding: .day, value: $0, to: today) }
        func at(_ day: Int, _ hour: Int, _ minute: Int = 0) -> Date {
            today.addingTimeInterval(TimeInterval(day * 86_400 + hour * 3_600 + minute * 60))
        }
        return MonarchPreviewMatrix {
            MonarchTimeGrid(
                days: week,
                events: [
                    MonarchAgendaEvent("Stand-up", id: "1", start: at(0, 9), end: at(0, 9, 15)),
                    MonarchAgendaEvent("Design review", id: "2", start: at(1, 9, 30), end: at(1, 11), detail: "Room 4"),
                    MonarchAgendaEvent("Interview", id: "3", start: at(1, 10), end: at(1, 11), status: .tentative),
                    MonarchAgendaEvent("Conference", id: "4", start: at(2, 0), isAllDay: true),
                ],
                workingHours: (1...5).map { MonarchTimeGridWorkingHours(weekday: $0, start: 9 * 60, end: 17 * 60) },
                unavailable: [
                    MonarchTimeGridUnavailable(id: "lunch", start: at(3, 12), end: at(3, 13), label: "Closed")
                ],
                onSelectSlot: { _ in },
                onSelectEvent: { _ in },
                onMoveEvent: { _ in }
            )
            .frame(height: MonarchTokens.Space.space24 * 4)
        }
    }
#endif
