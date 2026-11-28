import SwiftUI

/// The month grid as the web draws it: a title with a chevron that opens the month list, the previous and next
/// month, Monday-first weekday names, the days of the neighbouring months in a muted colour, dots under days with
/// events, today outlined and the selected day in the accent. It follows the calendar and locale of the
/// environment.
struct MonarchMiniMonthGrid: View {
    @Binding var selection: Date?
    let visibleMonth: Binding<Date>?
    let minimumDate: Date?
    let maximumDate: Date?
    let today: Date?
    let isDateDisabled: (@MainActor (Date) -> Bool)?
    let dayInfo: (@MainActor (Date) -> MonarchMiniMonthDayInfo?)?

    @Environment(\.calendar) private var calendar
    @Environment(\.monarchNow) private var clockNow
    @Environment(\.locale) private var locale
    @Environment(\.monarchTheme) private var theme
    @State private var ownMonth: Date?
    @ScaledMetric(relativeTo: .body) private var daySide: CGFloat = 40

    private var month: Date {
        let base = visibleMonth?.wrappedValue ?? ownMonth ?? selection ?? clockNow ?? Date()
        return calendar.dateInterval(of: .month, for: base)?.start ?? base
    }

    private func setMonth(_ date: Date) {
        let start = calendar.dateInterval(of: .month, for: date)?.start ?? date
        if let visibleMonth { visibleMonth.wrappedValue = start } else { ownMonth = start }
    }

    private func shifted(by months: Int) -> Date? {
        calendar.date(byAdding: .month, value: months, to: month)
    }

    private func isReachable(_ monthStart: Date?) -> Bool {
        guard let monthStart, let interval = calendar.dateInterval(of: .month, for: monthStart) else { return false }
        if let minimumDate, interval.end <= calendar.startOfDay(for: minimumDate) { return false }
        if let maximumDate, interval.start > maximumDate { return false }
        return true
    }

    /// The days shown: whole weeks, starting on the calendar's first weekday.
    private var days: [Date] {
        guard let interval = calendar.dateInterval(of: .month, for: month) else { return [] }
        let firstWeekday = calendar.component(.weekday, from: interval.start)
        let lead = (firstWeekday - calendar.firstWeekday + 7) % 7
        guard let start = calendar.date(byAdding: .day, value: -lead, to: interval.start),
            let last = calendar.date(byAdding: .day, value: -1, to: interval.end)
        else { return [] }
        let lastWeekday = calendar.component(.weekday, from: last)
        let trail = (calendar.firstWeekday + 6 - lastWeekday + 7) % 7
        guard let end = calendar.date(byAdding: .day, value: trail, to: last) else { return [] }
        var result: [Date] = []
        var current = start
        while current <= end {
            result.append(current)
            guard let next = calendar.date(byAdding: .day, value: 1, to: current) else { break }
            current = next
        }
        return result
    }

    private var weekdayNames: [String] {
        let names = calendar.shortWeekdaySymbols
        let first = calendar.firstWeekday - 1
        return Array(names[first...] + names[..<first])
    }

    var body: some View {
        VStack(spacing: theme.spacing.space2) {
            header
            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 0), count: 7), spacing: 2) {
                ForEach(weekdayNames, id: \.self) { name in
                    Text(name)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors.textMuted)
                        .accessibilityHidden(true)
                }
                ForEach(days, id: \.self) { date in
                    dayCell(date)
                }
            }
        }
    }

    private var header: some View {
        HStack(spacing: theme.spacing.space1) {
            stepButton(systemImage: "chevron.left", months: -1)
            Spacer(minLength: 0)
            Menu {
                ForEach(monthStarts, id: \.self) { start in
                    Button(start.formatted(Date.FormatStyle(locale: locale, calendar: calendar).month(.wide).year())) {
                        setMonth(start)
                    }
                }
            } label: {
                HStack(spacing: theme.spacing.space1) {
                    Text(month.formatted(Date.FormatStyle(locale: locale, calendar: calendar).month(.wide).year()))
                        .monarchTextStyle(.bodyStrong)
                        .foregroundStyle(theme.colors.textDefault)
                    Image(systemName: "chevron.down")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(theme.colors.textMuted)
                }
                .frame(minHeight: 44)
                .contentShape(Rectangle())
            }
            .accessibilityHint(Text(verbatim: ""))
            Spacer(minLength: 0)
            stepButton(systemImage: "chevron.right", months: 1)
        }
    }

    /// The months of the year on screen, for the title's menu.
    private var monthStarts: [Date] {
        guard let year = calendar.dateInterval(of: .year, for: month) else { return [] }
        return (0..<12).compactMap { calendar.date(byAdding: .month, value: $0, to: year.start) }
            .filter { isReachable($0) }
    }

    private func stepButton(systemImage: String, months: Int) -> some View {
        let target = shifted(by: months)
        return Button {
            if let target { setMonth(target) }
        } label: {
            Image(systemName: systemImage)
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(theme.colors.textMuted)
                .frame(width: 44, height: 44)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .disabled(!isReachable(target))
        .accessibilityLabel(months < 0 ? "Previous month" : "Next month")
    }

    @ViewBuilder private func dayCell(_ date: Date) -> some View {
        let isInMonth = calendar.isDate(date, equalTo: month, toGranularity: .month)
        let isSelected = MonarchMiniMonthTokens.isSameDay(date, selection, calendar: calendar)
        let isToday = calendar.isDate(date, inSameDayAs: today ?? clockNow ?? Date())
        let isSelectable = MonarchMiniMonthTokens.isSelectable(
            date,
            minimumDate: minimumDate,
            maximumDate: maximumDate,
            isDateDisabled: isDateDisabled,
            calendar: calendar
        )
        let info = dayInfo?(calendar.startOfDay(for: date))
        let dots = MonarchMiniMonthTokens.dotRoles(info)
        Button {
            selection = isSelected ? nil : calendar.startOfDay(for: date)
        } label: {
            Text(date.formatted(Date.FormatStyle(locale: locale, calendar: calendar).day()))
                .monarchTextStyle(isToday && !isSelected ? .bodyStrong : .body)
                .strikethrough(isInMonth && !isSelectable)
                .foregroundStyle(
                    isSelected
                        ? theme.colors.accentOnSolid
                        : (isInMonth && isSelectable ? theme.colors.textDefault : theme.colors.textDisabled)
                )
                .offset(y: dots.isEmpty ? 0 : -2)
                .frame(width: daySide - 6, height: daySide - 6)
                .background {
                    if isSelected {
                        MonarchShapes.control.fill(theme.colors[role: MonarchMiniMonthTokens.tint])
                    } else if isToday {
                        MonarchShapes.control.strokeBorder(theme.colors.borderDefault, lineWidth: 1)
                    }
                }
                // The dots sit inside the day's tile, under the number, in white on the selected day, as on the web.
                .overlay(alignment: .bottom) {
                    HStack(spacing: MonarchMiniMonthTokens.dotSpacing) {
                        ForEach(Array(dots.enumerated()), id: \.offset) { _, role in
                            Circle()
                                .fill(isSelected ? theme.colors.accentOnSolid : theme.colors[role: role])
                                .frame(width: MonarchMiniMonthTokens.dotSize, height: MonarchMiniMonthTokens.dotSize)
                        }
                    }
                    .padding(.bottom, 3)
                }
                .frame(maxWidth: .infinity, minHeight: 44)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .disabled(!isSelectable)
        .accessibilityLabel(
            [
                date.formatted(Date.FormatStyle(locale: locale, calendar: calendar).weekday(.wide).day().month(.wide)),
                info?.description,
            ]
            .compactMap { $0 }.joined(separator: ", ")
        )
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }
}
