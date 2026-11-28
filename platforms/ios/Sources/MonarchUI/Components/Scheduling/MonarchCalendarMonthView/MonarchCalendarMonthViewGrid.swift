import SwiftUI

/// What a month grid needs from its ``MonarchCalendarMonthView``.
struct MonarchCalendarMonthViewContext {
    var events: [MonarchAgendaEvent]
    var selection: Date?
    var today: Date
    var isFixedWeeks: Bool
    var maxEvents: Int
    var layout: MonarchCalendarMonthViewLayout
    var labels: MonarchCalendarMonthViewLabels
    var dayState: (@MainActor (Date) -> MonarchCalendarMonthViewDayState?)?
    var onSelectDay: @MainActor (Date) -> Void
    var onSelectEvent: (@MainActor (MonarchAgendaEvent) -> Void)?
    var onMoveEvent: (@MainActor (MonarchAgendaEvent, Date) -> Void)?
}

/// One month: the weekday header and the weeks.
struct MonarchCalendarMonthViewGrid: View {
    let month: Date
    let context: MonarchCalendarMonthViewContext

    @Environment(\.monarchTheme) private var theme
    @Environment(\.calendar) private var calendar
    @Environment(\.locale) private var locale
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    @State private var width: CGFloat = 0

    var body: some View {
        var calendar = self.calendar
        calendar.locale = locale
        let days = MonarchCalendarMonthViewTokens.days(
            month: month,
            calendar: calendar,
            isFixedWeeks: context.isFixedWeeks
        )
        let byDay = MonarchCalendarMonthViewTokens.eventsByDay(context.events, days: days, calendar: calendar)
        let showsEvents = MonarchCalendarMonthViewTokens.showsEvents(
            context.layout,
            cellWidth: width / 7,
            isAccessibilitySize: dynamicTypeSize.isAccessibilitySize
        )
        let weeks = stride(from: 0, to: days.count, by: 7).map { Array(days[$0..<min($0 + 7, days.count)]) }
        return VStack(spacing: 0) {
            HStack(spacing: 0) {
                ForEach(Array(MonarchCalendarMonthViewTokens.weekdays(calendar: calendar).enumerated()), id: \.offset) {
                    item in
                    Text(item.element.short)
                        .monarchTextStyle(.caption, weight: .semibold)
                        .lineLimit(1)
                        .minimumScaleFactor(0.7)
                        .foregroundStyle(theme.colors[role: MonarchCalendarMonthViewTokens.weekday])
                        .frame(minWidth: 0, maxWidth: .infinity)
                }
            }
            .padding(.bottom, MonarchCalendarMonthViewTokens.headerSpacing)
            // Every day's spoken name carries its weekday.
            .accessibilityHidden(true)
            ForEach(weeks, id: \.first) { week in
                HStack(spacing: 0) {
                    ForEach(week, id: \.self) { day in
                        MonarchCalendarMonthViewDay(
                            day: day,
                            events: byDay[day] ?? [],
                            isOutside: MonarchCalendarMonthViewTokens.isOutside(day, month: month, calendar: calendar),
                            isToday: calendar.isDate(day, inSameDayAs: context.today),
                            isSelected: context.selection.map { calendar.isDate(day, inSameDayAs: $0) } ?? false,
                            state: context.dayState?(day),
                            showsEvents: showsEvents,
                            context: context
                        )
                        .frame(minWidth: 0, maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
                    }
                }
                .fixedSize(horizontal: false, vertical: true)
                .overlay(alignment: .top) {
                    Rectangle()
                        .fill(theme.colors[role: MonarchCalendarMonthViewTokens.gridLine])
                        .frame(height: MonarchShapes.borderWidth)
                }
            }
        }
        .onGeometryChange(for: CGFloat.self, of: { $0.size.width }, action: { width = $0 })
        .accessibilityElement(children: .contain)
        .accessibilityLabel(MonarchCalendarMonthViewTokens.monthTitle(month, calendar: calendar, locale: locale))
    }
}
