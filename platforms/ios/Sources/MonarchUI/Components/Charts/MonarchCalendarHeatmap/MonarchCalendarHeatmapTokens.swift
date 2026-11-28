import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchCalendarHeatmap``.
///
/// Every function that needs the date takes the calendar (with its time
/// zone, first weekday and locale) and the locale as arguments, so nothing
/// reads the clock or the device settings.
enum MonarchCalendarHeatmapTokens {
    /// The number of days in a week.
    static let daysPerWeek = 7
    /// The side of a day in the year grid: 14 points, as on the web.
    static let yearCell: CGFloat = MonarchTokens.Space.space3 + MonarchTokens.Space.space0_5
    /// The smallest side a day shrinks to before the year grid scrolls sideways: 9 points.
    static let yearMinimumCell: CGFloat = MonarchTokens.Space.space2 + MonarchTokens.Space.px
    /// The gap between days.
    static let gap: CGFloat = MonarchTokens.Space.space0_5
    /// The corner radius of a day in the year grid.
    static let yearRadius: CGFloat = MonarchTokens.Space.space0_5
    /// The least side of a day in the month grid, before Dynamic Type: 44 points.
    static let monthMinimumCell: CGFloat = MonarchTokens.Space.space10 + MonarchTokens.Space.space1
    /// The largest width of a day in the month grid: 56 points.
    static let monthMaximumCell: CGFloat = MonarchTokens.Space.space12 + MonarchTokens.Space.space2
    /// The corner radius of a day in the month grid.
    static let monthRadius: CGFloat = MonarchTokens.Radius.sm
    /// The padding inside a day of the month grid.
    static let monthPadding: CGFloat = MonarchTokens.Space.space1
    /// The thin outline of a day without data or a non-working day.
    static let thinStroke: CGFloat = MonarchTokens.Space.px
    /// The outline of today and of the inspected day.
    static let thickStroke: CGFloat = MonarchTokens.Space.space0_5
    /// The dash and gap of a non-working day's outline.
    static let dash: [CGFloat] = [MonarchTokens.Space.space0_5, MonarchTokens.Space.space0_5]
    /// The diameter of the marker dot in the year grid.
    static let yearMarker: CGFloat = MonarchTokens.Space.space1
    /// The diameter of the marker dot in the month grid.
    static let monthMarker: CGFloat = MonarchTokens.Space.space1_5
    /// The room a month name needs in the year grid, before Dynamic Type.
    static let monthLabelWidth: CGFloat = MonarchTokens.Space.space8
    /// The gap between the weekday names and the year grid.
    static let labelSpacing: CGFloat = MonarchTokens.Space.space1_5
    /// The gap between the grid, the inspected day and the legend.
    static let sectionSpacing: CGFloat = MonarchTokens.Space.space2
    /// The least height kept for a year: 120 points, as the heatmap.
    static let yearMinimumHeight: CGFloat = MonarchHeatmapTokens.minimumHeight
    /// The least height kept for a month: 200 points.
    static let monthMinimumHeight: CGFloat = MonarchTokens.Space.space24 * 2 + MonarchTokens.Space.space2
    /// How far a value's text may shrink to fit its day.
    static let textMinimumScale: CGFloat = 0.6
    /// The type role of every text.
    static let textStyle: MonarchTextStyle = .caption

    /// The colour of the inspected day's outline.
    static var activeRole: MonarchColorRole { \.textDefault }
    /// The colour of today's outline.
    static var todayRole: MonarchColorRole { \.accentSolid }
    /// The colour of the outline of a day without data or a non-working day.
    static var emptyRole: MonarchColorRole { \.borderSubtle }
    /// The fill of a non-working day without a value.
    static var inactiveRole: MonarchColorRole { \.bgSubtle }
    /// The fill of a marker.
    /// The share of the grid width the leading fade covers.
    static let fadeShare: CGFloat = 0.06
    static var markerRole: MonarchColorRole { \.textDefault }
    /// The ring around a marker.
    static var markerRingRole: MonarchColorRole { \.bgSurface }
    /// The colour of a day number.
    static var dayNumberRole: MonarchColorRole { \.textDefault }
    /// The colour of a value in a day.
    static var valueRole: MonarchColorRole { \.textMuted }
    /// The chip behind text on the two strongest levels.
    static var chipRole: MonarchColorRole { \.bgSurface }

    // MARK: Grid

    /// The days of the range, as positions in a grid of whole weeks.
    struct Model: Equatable {
        /// The start of the first day.
        let start: Date
        /// The number of days in the range.
        let count: Int
        /// The empty places before the first day in its week.
        let lead: Int
        /// The number of weeks the range touches.
        let weeks: Int
        /// The entries by day, counted from the first day. Only days in the range.
        let days: [Int: MonarchCalendarHeatmapDay]

        /// The entry of a day, or a day without data.
        func day(_ offset: Int, calendar: Calendar) -> MonarchCalendarHeatmapDay {
            days[offset]
                ?? MonarchCalendarHeatmapDay(
                    MonarchCalendarHeatmapTokens.date(offset, from: start, calendar: calendar),
                    value: nil
                )
        }

        /// The day at a place of the grid, counted week by week, or `nil` for
        /// a place before the first or after the last day.
        func offset(atPlace place: Int) -> Int? {
            let offset = place - lead
            return offset >= 0 && offset < count ? offset : nil
        }
    }

    /// The days from `start` to the start of the day of `date`, in the calendar.
    static func offset(of date: Date, from start: Date, calendar: Calendar) -> Int {
        calendar.dateComponents([.day], from: start, to: calendar.startOfDay(for: date)).day ?? 0
    }

    /// The start of the day `offset` days after `start`.
    static func date(_ offset: Int, from start: Date, calendar: Calendar) -> Date {
        calendar.date(byAdding: .day, value: offset, to: start) ?? start
    }

    /// The place of a day in its week, 0 for the calendar's first weekday.
    static func weekdayIndex(_ date: Date, calendar: Calendar) -> Int {
        (calendar.component(.weekday, from: date) - calendar.firstWeekday + daysPerWeek) % daysPerWeek
    }

    /// The grid of the range: `from` to `to`, or the first to the last entry
    /// when they are `nil`. `nil` when there is no range, or it ends before it starts.
    static func model(
        days: [MonarchCalendarHeatmapDay],
        from: Date?,
        to: Date?,
        calendar: Calendar
    ) -> Model? {
        let starts = days.map { calendar.startOfDay(for: $0.date) }
        guard
            let first = from.map({ calendar.startOfDay(for: $0) }) ?? starts.min(),
            let last = to.map({ calendar.startOfDay(for: $0) }) ?? starts.max()
        else { return nil }
        let count = offset(of: last, from: first, calendar: calendar) + 1
        guard count > 0 else { return nil }
        var byOffset: [Int: MonarchCalendarHeatmapDay] = [:]
        for day in days {
            let index = offset(of: day.date, from: first, calendar: calendar)
            if index >= 0 && index < count { byOffset[index] = day }
        }
        let lead = weekdayIndex(first, calendar: calendar)
        let weeks = (lead + count + daysPerWeek - 1) / daysPerWeek
        return Model(start: first, count: count, lead: lead, weeks: weeks, days: byOffset)
    }

    /// The column and row of a day: weeks as columns in the year grid, weeks
    /// as rows in the month grid.
    static func position(
        _ offset: Int,
        lead: Int,
        layout: MonarchCalendarHeatmapLayout
    ) -> (column: Int, row: Int) {
        let place = lead + offset
        switch layout {
        case .year: return (place / daysPerWeek, place % daysPerWeek)
        case .month: return (place % daysPerWeek, place / daysPerWeek)
        }
    }

    /// The short weekday names from the calendar's first weekday.
    static func weekdaySymbols(_ calendar: Calendar, veryShort: Bool = false) -> [String] {
        let symbols = veryShort ? calendar.veryShortStandaloneWeekdaySymbols : calendar.shortStandaloneWeekdaySymbols
        guard symbols.count == daysPerWeek else { return symbols }
        return (0..<daysPerWeek).map { symbols[(calendar.firstWeekday - 1 + $0) % daysPerWeek] }
    }

    /// Whether a row of the year grid names its weekday: every other row,
    /// starting with the second, as on the web.
    static func showsWeekday(row: Int) -> Bool {
        row % 2 == 1
    }

    /// The month names of the year grid by column: at the first day and at
    /// each first of a month, skipped when closer than `minimumColumns` to
    /// the previous name.
    static func monthLabels(_ model: Model, calendar: Calendar, minimumColumns: Int) -> [Int: String] {
        let names = calendar.shortStandaloneMonthSymbols
        var labels: [Int: String] = [:]
        var previous: Int?
        for offset in 0..<model.count {
            let date = date(offset, from: model.start, calendar: calendar)
            guard offset == 0 || calendar.component(.day, from: date) == 1 else { continue }
            let column = position(offset, lead: model.lead, layout: .year).column
            if let previous, column - previous < minimumColumns { continue }
            let month = calendar.component(.month, from: date)
            guard names.indices.contains(month - 1) else { continue }
            labels[column] = names[month - 1]
            previous = column
        }
        return labels
    }

    /// The number of columns a month name needs.
    static func minimumLabelColumns(labelWidth: CGFloat, step: CGFloat) -> Int {
        guard step > 0 else { return 1 }
        return max(1, Int((labelWidth / step).rounded(.up)))
    }

    /// The day after a VoiceOver adjustment: one day on; the first swipe goes
    /// to the first day (or the last, backwards). Stops at the ends.
    static func adjusted(_ offset: Int?, forward: Bool, count: Int) -> Int? {
        guard count > 0 else { return nil }
        guard let offset else { return forward ? 0 : count - 1 }
        return min(count - 1, max(0, offset + (forward ? 1 : -1)))
    }

    // MARK: Appearance

    /// The fill of a day.
    enum Fill: Equatable {
        /// A level of the scale, 0 to 4.
        case level(Int)
        /// No fill: a day without data.
        case none
        /// The fill of a non-working day without a value.
        case inactive
    }

    /// How a day is drawn.
    struct CellStyle: Equatable {
        /// The fill.
        let fill: Fill
        /// The outline colour, or `nil` for none.
        let stroke: MonarchColorRole?
        /// The outline width.
        let strokeWidth: CGFloat
        /// Whether the outline is dashed.
        let isDashed: Bool
    }

    /// How a day is drawn. The inspected outline wins over today's; a
    /// non-working day without a value above zero is dashed; a day without
    /// data is outlined and empty; zero is the muted background.
    static func cellStyle(level: Int?, isInactive: Bool, isToday: Bool, isActive: Bool) -> CellStyle {
        let isQuiet = isInactive && (level ?? 0) == 0
        let fill: Fill = isQuiet ? .inactive : level.map { .level($0) } ?? .none
        if isActive {
            return CellStyle(fill: fill, stroke: activeRole, strokeWidth: thickStroke, isDashed: false)
        }
        if isToday {
            return CellStyle(fill: fill, stroke: todayRole, strokeWidth: thickStroke, isDashed: false)
        }
        if isQuiet {
            return CellStyle(fill: fill, stroke: emptyRole, strokeWidth: thinStroke, isDashed: true)
        }
        if level == nil {
            return CellStyle(fill: fill, stroke: emptyRole, strokeWidth: thinStroke, isDashed: false)
        }
        return CellStyle(fill: fill, stroke: nil, strokeWidth: 0, isDashed: false)
    }

    // MARK: Text

    /// The format of a full date: "Mon, Mar 2, 2026" in English.
    static func dateStyle(calendar: Calendar, locale: Locale) -> Date.FormatStyle {
        Date.FormatStyle(
            date: .abbreviated,
            time: .omitted,
            locale: locale,
            calendar: calendar,
            timeZone: calendar.timeZone
        )
        .weekday(.abbreviated)
    }

    /// A full date: "Mon, Mar 2, 2026" in English.
    static func dateText(_ date: Date, calendar: Calendar, locale: Locale) -> String {
        date.formatted(dateStyle(calendar: calendar, locale: locale))
    }

    /// The day of the month as a number.
    static func dayNumber(_ date: Date, calendar: Calendar, locale: Locale) -> String {
        calendar.component(.day, from: date).formatted(.number.locale(locale))
    }

    /// The value as written: the day's own text, the formatted value, or the no-value text.
    static func valueText(_ day: MonarchCalendarHeatmapDay, format: (Double) -> String, noValue: String) -> String {
        if let label = day.valueLabel { return label }
        guard let value = day.value, value.isFinite else { return noValue }
        return format(value)
    }

    /// What a day of the month grid shows under its number: its own text,
    /// a value other than zero, or its note. `nil` for nothing.
    static func monthCellText(_ day: MonarchCalendarHeatmapDay, format: (Double) -> String) -> String? {
        if let label = day.valueLabel { return label }
        if let value = day.value, value.isFinite, value != 0 { return format(value) }
        guard let note = day.note, !note.isEmpty else { return nil }
        return note
    }

    /// The value with the day's note, and "Today", as VoiceOver reads it.
    static func spokenValue(
        _ day: MonarchCalendarHeatmapDay,
        isToday: Bool,
        format: (Double) -> String,
        noValue: String,
        today: String
    ) -> String {
        var parts = [valueText(day, format: format, noValue: noValue)]
        if let note = day.note, !note.isEmpty { parts.append(note) }
        if isToday { parts.append(today) }
        return parts.joined(separator: ", ")
    }

    /// A day as read in the inspection row and by VoiceOver: "Mon, Mar 2, 2026: 8h, Holiday".
    static func describe(date: String, value: String) -> String {
        "\(date): \(value)"
    }

    /// The day with the highest value, the first one on a tie.
    static func highest(_ model: Model) -> Int? {
        model.days
            .filter { $0.value.value?.isFinite == true }
            .sorted { $0.key < $1.key }
            .max { ($0.value.value ?? 0) < ($1.value.value ?? 0) }?
            .key
    }

    /// The sum of every finite value in the range.
    static func total(_ model: Model) -> Double {
        model.days.values.reduce(0) { sum, day in
            guard let value = day.value, value.isFinite else { return sum }
            return sum + value
        }
    }

    /// Whether some day of the range has no value, so the legend shows the no-data step.
    static func hasMissingDays(_ model: Model) -> Bool {
        model.days.values.filter { $0.value?.isFinite == true }.count < model.count
    }

    /// The generated summary: the days, the range, the total and the highest day.
    static func summary(
        count: String,
        first: String,
        last: String,
        total: String,
        highest: String?,
        labels: MonarchCalendarHeatmapLabels
    ) -> String {
        let text = MonarchCalendarHeatmapStrings.fill(
            labels.summary ?? MonarchCalendarHeatmapStrings.summary,
            count,
            first,
            last,
            total
        )
        guard let highest else { return text }
        return text + ". "
            + MonarchCalendarHeatmapStrings.fill(labels.highest ?? MonarchCalendarHeatmapStrings.highest, highest)
    }

    /// The number of days with its noun.
    static func dayCount(_ count: Int, text: String, labels: MonarchCalendarHeatmapLabels) -> String {
        MonarchCalendarHeatmapStrings.fill(
            count == 1
                ? labels.dayCountOne ?? MonarchCalendarHeatmapStrings.dayCountOne
                : labels.dayCount ?? MonarchCalendarHeatmapStrings.dayCount,
            text
        )
    }

    // MARK: State and tables

    /// The status shown: empty when the chart is ready but no day of the range has an entry.
    static func status(_ status: MonarchChartFrameStatus, model: Model?) -> MonarchChartFrameStatus {
        status == .ready && (model?.days.isEmpty ?? true) ? .empty : status
    }

    /// The labels of the heatmap legend, from the calendar heatmap's.
    static func legendLabels(_ labels: MonarchCalendarHeatmapLabels) -> MonarchHeatmapLabels {
        MonarchHeatmapLabels(
            frame: labels.frame,
            less: labels.less,
            more: labels.more,
            noData: labels.noData,
            rangeBelow: labels.rangeBelow,
            range: labels.range,
            rangeAbove: labels.rangeAbove
        )
    }

    /// The days with an entry, in date order.
    static func entries(_ model: Model) -> [(offset: Int, day: MonarchCalendarHeatmapDay)] {
        model.days.keys.sorted().compactMap { offset in model.days[offset].map { (offset, $0) } }
    }

    /// The data given to VoiceOver as an audio graph: one point per day
    /// with a value, along a date axis.
    static func audioGraphTable(
        _ model: Model,
        dateText: (Int) -> String,
        format: (Double) -> String,
        noValue: String,
        labels: MonarchCalendarHeatmapLabels
    ) -> MonarchChartTable {
        let entries = entries(model)
        return MonarchChartTable(
            categoryTitle: labels.date ?? MonarchCalendarHeatmapStrings.date,
            categories: entries.map { dateText($0.offset) },
            series: [
                MonarchChartTableSeries(
                    labels.value ?? MonarchCalendarHeatmapStrings.value,
                    values: entries.map { $0.day.value.flatMap { $0.isFinite ? $0 : nil } },
                    formattedValues: entries.map { valueText($0.day, format: format, noValue: noValue) }
                )
            ],
            valueTitle: labels.value ?? MonarchCalendarHeatmapStrings.value
        )
    }

    /// The table of "View as table": the date, the value and, when any day
    /// has one, the note.
    ///
    /// The shared table shows a cell only when it has a number, so a day
    /// with text but no number (a value label without a value, or a note) is
    /// given zero as a stand-in; the text is what is written and read.
    static func displayTable(
        _ model: Model,
        dateText: (Int) -> String,
        format: (Double) -> String,
        noValue: String,
        labels: MonarchCalendarHeatmapLabels
    ) -> MonarchChartTable {
        let entries = entries(model)
        var series = [
            MonarchChartTableSeries(
                labels.value ?? MonarchCalendarHeatmapStrings.value,
                values: entries.map { entry in
                    if let value = entry.day.value, value.isFinite { return value }
                    return entry.day.valueLabel == nil ? nil : 0
                },
                formattedValues: entries.map { valueText($0.day, format: format, noValue: noValue) }
            )
        ]
        if entries.contains(where: { !($0.day.note ?? "").isEmpty }) {
            series.append(
                MonarchChartTableSeries(
                    labels.note ?? MonarchCalendarHeatmapStrings.note,
                    values: entries.map { ($0.day.note ?? "").isEmpty ? nil : 0 },
                    formattedValues: entries.map { $0.day.note ?? "" }
                )
            )
        }
        return MonarchChartTable(
            categoryTitle: labels.date ?? MonarchCalendarHeatmapStrings.date,
            categories: entries.map { dateText($0.offset) },
            series: series
        )
    }
}
