import SwiftUI

/// One day of the month grid: the date, a badge, and the day's events as
/// chips or dots.
struct MonarchCalendarMonthViewDay: View {
    let day: Date
    let events: [MonarchAgendaEvent]
    let isOutside: Bool
    let isToday: Bool
    let isSelected: Bool
    let state: MonarchCalendarMonthViewDayState?
    let showsEvents: Bool
    let context: MonarchCalendarMonthViewContext

    @Environment(\.monarchTheme) private var theme
    @Environment(\.calendar) private var calendar
    @Environment(\.locale) private var locale
    @ScaledMetric(relativeTo: .caption) private var dotSize = MonarchCalendarMonthViewTokens.dotSize
    @ScaledMetric(relativeTo: .caption) private var markSize = MonarchCalendarMonthViewTokens.dateMarkSize
    @ScaledMetric(relativeTo: .caption) private var eventsHeight = MonarchCalendarMonthViewTokens.eventsCellHeight
    @State private var isTargeted = false
    @State private var showsAll = false

    private var label: String {
        MonarchCalendarMonthViewTokens.dayLabel(
            date: MonarchCalendarMonthViewTokens.longDate(day, calendar: calendar, locale: locale),
            count: events.count,
            isToday: isToday,
            badge: state?.badge,
            locale: locale,
            labels: context.labels
        )
    }

    var body: some View {
        Group {
            if showsEvents { eventsCell } else { dotsCell }
        }
        .background {
            if let role = MonarchCalendarMonthViewTokens.cellBackground(
                isSelected: isSelected,
                isTargeted: isTargeted,
                isUnavailable: state?.isUnavailable ?? false
            ) {
                Rectangle().fill(theme.colors[role: role])
            }
        }
        .overlay {
            if state?.isUnavailable ?? false {
                MonarchTimeGridHatch(spacing: MonarchTimeGridTokens.hatchSpacing)
                    .stroke(
                        theme.colors[role: MonarchTimeGridTokens.unavailableHatch].opacity(0.5),
                        lineWidth: MonarchShapes.borderWidth
                    )
                    .clipped()
                    .allowsHitTesting(false)
            }
        }
        .overlay {
            if isSelected {
                Rectangle().strokeBorder(
                    theme.colors[role: MonarchCalendarMonthViewTokens.selectedBorder],
                    lineWidth: MonarchShapes.borderWidth
                )
            }
        }
    }

    // MARK: Date

    private var dateText: some View {
        Text(MonarchCalendarMonthViewTokens.dayNumber(day, calendar: calendar, locale: locale))
            .monarchTextStyle(.label)
            .monospacedDigit()
            .foregroundStyle(
                theme.colors[
                    role: MonarchCalendarMonthViewTokens.dateForeground(
                        isToday: isToday,
                        isOutside: isOutside,
                        isUnavailable: state?.isUnavailable ?? false
                    )
                ]
            )
            .frame(minWidth: markSize, minHeight: markSize)
            .background {
                if isToday { Circle().fill(theme.colors[role: MonarchCalendarMonthViewTokens.todayMark]) }
            }
    }

    private var badge: some View {
        Group {
            if let badge = state?.badge, !badge.isEmpty {
                Text(badge)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors[role: MonarchCalendarMonthViewTokens.secondary])
                    .lineLimit(1)
                    .accessibilityHidden(true)
            }
        }
    }

    // MARK: Dots

    private var dotsCell: some View {
        Button {
            context.onSelectDay(day)
        } label: {
            VStack(spacing: MonarchCalendarMonthViewTokens.cellSpacing) {
                dateText
                HStack(spacing: MonarchCalendarMonthViewTokens.dotSpacing) {
                    ForEach(events.prefix(MonarchCalendarMonthViewTokens.maxDots)) { event in
                        dot(event)
                    }
                }
                .frame(minHeight: dotSize)
                badge
            }
            .padding(.vertical, MonarchCalendarMonthViewTokens.cellPadding)
            .frame(maxWidth: .infinity, minHeight: MonarchCalendarMonthViewTokens.dotsCellHeight)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(label)
        .accessibilityAddTraits(isSelected ? [.isButton, .isSelected] : .isButton)
    }

    @ViewBuilder
    private func dot(_ event: MonarchAgendaEvent) -> some View {
        let fill =
            event.status == .cancelled || event.status == .declined
            ? theme.colors[role: MonarchCalendarMonthViewTokens.dotRole(event.status)]
            : (event.color ?? theme.colors[role: MonarchCalendarMonthViewTokens.dotRole(event.status)])
        if event.status == .tentative {
            Circle().strokeBorder(fill, lineWidth: MonarchShapes.borderWidth).frame(width: dotSize, height: dotSize)
        } else {
            Circle().fill(fill).frame(width: dotSize, height: dotSize)
        }
    }

    // MARK: Events

    private var eventsCell: some View {
        let counts = MonarchCalendarMonthViewTokens.visibleCount(events.count, maxEvents: context.maxEvents)
        return VStack(alignment: .leading, spacing: MonarchCalendarMonthViewTokens.cellSpacing) {
            ViewThatFits(in: .horizontal) {
                dateRow(HStackLayout(alignment: .center, spacing: MonarchCalendarMonthViewTokens.cellSpacing))
                dateRow(VStackLayout(alignment: .leading, spacing: 0))
            }
            ForEach(events.prefix(counts.shown)) { event in
                chip(event, variant: .dot)
            }
            if counts.hidden > 0 {
                Button(MonarchCalendarMonthViewTokens.more(counts.hidden, locale: locale, labels: context.labels)) {
                    showsAll = true
                }
                .buttonStyle(.plain)
                .monarchTextStyle(.caption)
                .lineLimit(1)
                .minimumScaleFactor(0.8)
                .foregroundStyle(theme.colors[role: MonarchCalendarMonthViewTokens.more])
                .monarchMinimumHitTarget()
                .popover(isPresented: $showsAll) {
                    VStack(alignment: .leading, spacing: MonarchCalendarMonthViewTokens.cellSpacing) {
                        Text(MonarchCalendarMonthViewTokens.longDate(day, calendar: calendar, locale: locale))
                            .monarchTextStyle(.subheading)
                            .foregroundStyle(theme.colors.textDefault)
                            .accessibilityAddTraits(.isHeader)
                        ForEach(events) { event in
                            chip(event, variant: .bar)
                        }
                    }
                    .padding()
                    .presentationCompactAdaptation(.popover)
                }
            }
        }
        .padding(MonarchCalendarMonthViewTokens.cellPadding)
        .frame(maxWidth: .infinity, minHeight: eventsHeight, alignment: .topLeading)
        .contentShape(Rectangle())
        .dropDestination(
            for: String.self,
            action: { ids, _ in
                guard let onMoveEvent = context.onMoveEvent,
                    let event = context.events.first(where: { ids.contains($0.id) })
                else { return false }
                onMoveEvent(event, day)
                return true
            },
            isTargeted: { isTargeted = $0 && context.onMoveEvent != nil }
        )
        .accessibilityElement(children: .contain)
    }

    private func dateRow(_ layout: some Layout) -> some View {
        layout {
            Button {
                context.onSelectDay(day)
            } label: {
                dateText.monarchMinimumHitTarget()
            }
            .buttonStyle(.plain)
            .accessibilityLabel(label)
            .accessibilityAddTraits(isSelected ? .isSelected : [])
            badge
        }
    }

    private func selectAction(_ event: MonarchAgendaEvent) -> (@MainActor () -> Void)? {
        guard let select = context.onSelectEvent else { return nil }
        return { select(event) }
    }

    private func makeChip(_ event: MonarchAgendaEvent, variant: MonarchEventChipVariant, time: String?) -> some View {
        MonarchEventChip(
            event.title,
            time: time,
            detail: variant == .bar ? event.detail : nil,
            color: event.color,
            status: event.status,
            variant: variant,
            isCompact: true,
            action: selectAction(event)
        )
    }

    @ViewBuilder
    private func chip(_ event: MonarchAgendaEvent, variant: MonarchEventChipVariant) -> some View {
        // The time and the title when both fit the cell, the title alone when the cell is too narrow for both.
        let timeText = MonarchCalendarMonthViewTokens.chipTime(event, on: day, calendar: calendar, locale: locale)
        let chip = ViewThatFits(in: .horizontal) {
            makeChip(event, variant: variant, time: timeText)
            makeChip(event, variant: variant, time: nil)
        }
        if let onMoveEvent = context.onMoveEvent {
            chip.draggable(event.id)
                .accessibilityActions {
                    ForEach(MonarchCalendarMonthViewTokens.moveActions(labels: context.labels), id: \.days) { action in
                        Button(action.name) {
                            if let target = MonarchCalendarMonthViewTokens.movedDay(
                                from: day,
                                days: action.days,
                                calendar: calendar
                            ) {
                                onMoveEvent(event, target)
                            }
                        }
                    }
                }
        } else {
            chip
        }
    }
}
