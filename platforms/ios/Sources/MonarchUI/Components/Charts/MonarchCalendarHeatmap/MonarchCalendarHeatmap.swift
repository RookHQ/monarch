import SwiftUI

/// Values by day on a calendar grid: a contribution-style year or a month
/// calendar, such as activity, sign-ins or hours logged.
///
/// ```swift
/// MonarchCalendarHeatmap(
///     "Contributions",
///     days: activity.map { MonarchCalendarHeatmapDay($0.date, value: Double($0.count)) },
///     from: yearStart,
///     to: today,
///     today: today
/// )
/// ```
///
/// Days are read as calendar days in the environment's `calendar`, which
/// also gives the first weekday, the time zone and the weekday and month
/// names; set `.environment(\.calendar, …)` to follow a workspace setting.
/// Pass `today` from the app: the heatmap never reads the clock. Values are
/// binned into the same four steps as ``MonarchHeatmap`` with its "Less …
/// More" legend; zero is the muted background, a day without data is
/// outlined and empty, a non-working day is dashed, today has an accent
/// outline and a marker is a dot. The year scrolls sideways on a phone and
/// opens at its latest week. Tapping a day outlines it and writes its date,
/// value and note under the grid; with `onSelect`, it also selects it.
///
/// Accessibility: in the year layout the grid is one adjustable element
/// named by the generated summary (the days, range, total and highest day);
/// swiping up and down moves day by day and reads each one, and a double tap
/// selects it with `onSelect`. In the month layout each day is its own
/// element, read as its date with its value, note and "Today" as the value.
/// Both carry an accessibility chart descriptor for Audio Graphs. "View as
/// table" lists every day with an entry as text.
public struct MonarchCalendarHeatmap: View {
    private typealias Tokens = MonarchCalendarHeatmapTokens

    private let title: String
    private let days: [MonarchCalendarHeatmapDay]
    private let from: Date?
    private let to: Date?
    private let description: String?
    private let layout: MonarchCalendarHeatmapLayout
    private let today: Date?
    private let status: MonarchChartFrameStatus
    private let color: MonarchChartColor
    private let thresholds: [Double]?
    private let showsValues: Bool
    private let showsLegend: Bool
    private let summary: String?
    private let emptyMessage: String?
    private let footer: String?
    private let labels: MonarchCalendarHeatmapLabels
    private let formatValue: ((Double) -> String)?
    private let onRetry: (@MainActor () -> Void)?
    private let onSelect: (@MainActor (MonarchCalendarHeatmapDay) -> Void)?

    @State private var active: Int?
    @Environment(\.monarchTheme) private var theme
    @Environment(\.calendar) private var calendar
    @Environment(\.locale) private var locale

    /// A calendar heatmap.
    ///
    /// - Parameters:
    ///   - title: The name of the chart, already localised.
    ///   - days: One entry per day that has something to show, in any order.
    ///     Days in the range without an entry are "no data".
    ///   - from: The first day drawn. The earliest entry when `nil`.
    ///   - to: The last day drawn. The latest entry when `nil`.
    ///   - description: One line under the title, such as the period.
    ///   - layout: A year of weeks, or a month calendar.
    ///   - today: Today, outlined. Pass it from the app; nothing is outlined when `nil`.
    ///   - status: Whether the chart, a placeholder, an empty state or an error
    ///     shows. A ready chart without an entry in the range shows the empty state.
    ///   - color: The data colour of the steps.
    ///   - thresholds: Three ascending values where levels 2, 3 and 4 start.
    ///     Quarters of the largest value when `nil`.
    ///   - showsValues: Month layout: whether each day's value is written under its number.
    ///   - showsLegend: Whether the "Less … More" legend shows under the grid.
    ///   - summary: Replaces the generated summary that VoiceOver reads.
    ///   - emptyMessage: The empty-state message.
    ///   - footer: Notes under the chart, such as the source.
    ///   - labels: Replacements for the built-in strings.
    ///   - formatValue: Writes a value. The default is a number in the environment's locale.
    ///   - onRetry: Shows Try again in the error status and runs when it is pressed.
    ///   - onSelect: Runs with the day (its value `nil` without an entry) when a
    ///     day is tapped or selected with VoiceOver.
    public init(
        _ title: String,
        days: [MonarchCalendarHeatmapDay],
        from: Date? = nil,
        to: Date? = nil,
        description: String? = nil,
        layout: MonarchCalendarHeatmapLayout = .year,
        today: Date? = nil,
        status: MonarchChartFrameStatus = .ready,
        color: MonarchChartColor = .data1,
        thresholds: [Double]? = nil,
        showsValues: Bool = true,
        showsLegend: Bool = true,
        summary: String? = nil,
        emptyMessage: String? = nil,
        footer: String? = nil,
        labels: MonarchCalendarHeatmapLabels = MonarchCalendarHeatmapLabels(),
        formatValue: ((Double) -> String)? = nil,
        onRetry: (@MainActor () -> Void)? = nil,
        onSelect: (@MainActor (MonarchCalendarHeatmapDay) -> Void)? = nil
    ) {
        self.title = title
        self.days = days
        self.from = from
        self.to = to
        self.description = description
        self.layout = layout
        self.today = today
        self.status = status
        self.color = color
        self.thresholds = thresholds
        self.showsValues = showsValues
        self.showsLegend = showsLegend
        self.summary = summary
        self.emptyMessage = emptyMessage
        self.footer = footer
        self.labels = labels
        self.formatValue = formatValue
        self.onRetry = onRetry
        self.onSelect = onSelect
    }

    private var noValue: String { labels.frame.noValue ?? MonarchChartFrameStrings.noValue }

    private func format(_ value: Double) -> String {
        formatValue?(value) ?? value.formatted(.number.locale(locale))
    }

    /// The content of the view.
    public var body: some View {
        let model = Tokens.model(days: days, from: from, to: to, calendar: calendar)
        let resolved = Tokens.status(status, model: model)
        let table =
            model.map {
                Tokens.displayTable($0, dateText: dateText(in: $0), format: format, noValue: noValue, labels: labels)
            } ?? MonarchChartTable(categoryTitle: "", categories: [], series: [])
        MonarchChartDataFrame(
            title: title,
            description: description,
            status: resolved,
            table: table,
            legend: [],
            hiddenSeries: nil,
            legendMarker: .square,
            emptyMessage: emptyMessage,
            footer: footer,
            height: layout == .year ? Tokens.yearMinimumHeight : Tokens.monthMinimumHeight,
            isTableToggleShown: true,
            labels: labels.frame,
            onRetry: onRetry
        ) {
            if let model {
                chart(model)
            }
        }
    }

    private func dateText(in model: Tokens.Model) -> (Int) -> String {
        let calendar = self.calendar
        let locale = self.locale
        return { offset in
            Tokens.dateText(
                Tokens.date(offset, from: model.start, calendar: calendar),
                calendar: calendar,
                locale: locale
            )
        }
    }

    private func todayOffset(_ model: Tokens.Model) -> Int? {
        guard let today else { return nil }
        let offset = Tokens.offset(of: today, from: model.start, calendar: calendar)
        return offset >= 0 && offset < model.count ? offset : nil
    }

    private func spokenValue(_ offset: Int, model: Tokens.Model) -> String {
        Tokens.spokenValue(
            model.day(offset, calendar: calendar),
            isToday: offset == todayOffset(model),
            format: format,
            noValue: noValue,
            today: labels.today ?? MonarchCalendarHeatmapStrings.today
        )
    }

    private func describe(_ offset: Int, model: Tokens.Model) -> String {
        Tokens.describe(date: dateText(in: model)(offset), value: spokenValue(offset, model: model))
    }

    private func generatedSummary(_ model: Tokens.Model) -> String {
        let text = dateText(in: model)
        return Tokens.summary(
            count: Tokens.dayCount(model.count, text: model.count.formatted(.number.locale(locale)), labels: labels),
            first: text(0),
            last: text(model.count - 1),
            total: format(Tokens.total(model)),
            highest: Tokens.highest(model).map { describe($0, model: model) },
            labels: labels
        )
    }

    @ViewBuilder private func chart(_ model: Tokens.Model) -> some View {
        let scale = MonarchHeatmapTokens.scale(values: days.map(\.value), thresholds: thresholds)
        let base = MonarchHeatmapTokens.mix(
            theme.colors.tokens.bgSurface,
            into: theme.colors.tokens[keyPath: MonarchChartLegendTokens.role(color)],
            amount: MonarchBarChartTokens.softening
        )
        let current = active.flatMap { $0 < model.count ? $0 : nil }
        let spoken = summary ?? generatedSummary(model)
        let descriptor = MonarchChartFrameDescriptor(
            title: title,
            summary: spoken,
            table: Tokens.audioGraphTable(
                model,
                dateText: dateText(in: model),
                format: format,
                noValue: noValue,
                labels: labels
            )
        )
        VStack(alignment: .leading, spacing: Tokens.sectionSpacing) {
            switch layout {
            case .year:
                MonarchCalendarHeatmapYearGrid(
                    model: model,
                    calendar: calendar,
                    scale: scale,
                    base: base,
                    today: todayOffset(model),
                    active: current,
                    onTap: { select($0, model: model) }
                )
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(spoken)
                .accessibilityValue(current.map { describe($0, model: model) } ?? "")
                .accessibilityAddTraits(onSelect == nil ? [] : .isButton)
                .accessibilityAdjustableAction { direction in
                    active = Tokens.adjusted(current, forward: direction == .increment, count: model.count)
                }
                .accessibilityAction {
                    if let current { select(current, model: model) }
                }
                .accessibilityChartDescriptor(descriptor)
            case .month:
                MonarchCalendarHeatmapMonthGrid(
                    model: model,
                    calendar: calendar,
                    locale: locale,
                    scale: scale,
                    base: base,
                    today: todayOffset(model),
                    active: current,
                    showsValues: showsValues,
                    isSelectable: onSelect != nil,
                    format: format,
                    dateText: dateText(in: model),
                    spokenValue: { spokenValue($0, model: model) },
                    onTap: { select($0, model: model) }
                )
                .accessibilityElement(children: .contain)
                .accessibilityLabel(spoken)
                .accessibilityChartDescriptor(descriptor)
            }
            if let current {
                Text(describe(current, model: model))
                    .monarchTextStyle(Tokens.textStyle)
                    .foregroundStyle(theme.colors.textDefault)
                    .fixedSize(horizontal: false, vertical: true)
                    .accessibilityHidden(true)
            }
            if showsLegend {
                let missing = Tokens.hasMissingDays(model)
                let legendLabels = Tokens.legendLabels(labels)
                MonarchHeatmapLegend(
                    color: color,
                    showsNoData: missing,
                    steps: MonarchHeatmapTokens.legendSteps(
                        scale: scale,
                        format: format,
                        showsNoData: missing,
                        labels: legendLabels
                    ),
                    labels: legendLabels
                )
            }
        }
    }

    private func select(_ offset: Int, model: Tokens.Model) {
        active = offset
        onSelect?(model.day(offset, calendar: calendar))
    }
}

#if DEBUG
    #Preview("MonarchCalendarHeatmap") {
        let start = Calendar.current.startOfDay(for: Date(timeIntervalSinceReferenceDate: 0))
        let days = (0..<120).map { offset in
            MonarchCalendarHeatmapDay(
                Calendar.current.date(byAdding: .day, value: offset, to: start) ?? start,
                value: Double((offset * 7) % 11)
            )
        }
        MonarchPreviewMatrix {
            VStack {
                MonarchCalendarHeatmap("Contributions", days: days, today: days[119].date)
                MonarchCalendarHeatmap(
                    "Hours",
                    days: Array(days.prefix(31)),
                    layout: .month,
                    today: days[10].date,
                    color: .data2
                )
            }
        }
    }
#endif
