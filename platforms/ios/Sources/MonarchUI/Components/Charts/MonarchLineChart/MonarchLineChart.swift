import SwiftUI

/// A line or area chart for trends over time or ordered categories, with
/// several series, gaps, reference lines and touch inspection, in a
/// ``MonarchChartFrame``.
///
/// ```swift
/// MonarchLineChart(
///     "Sessions",
///     dates: days,
///     series: [
///         MonarchLineChartSeries("current", label: "This period", values: sessions),
///         MonarchLineChartSeries("previous", label: "Previous period", values: previous, color: .neutral, isDashed: true),
///     ],
///     description: "Last 14 days against the previous 14",
///     referenceLines: [MonarchLineChartReferenceLine(1_200, label: "Target")]
/// )
/// ```
///
/// Built on Swift Charts: each series is a `LineMark` (with an `AreaMark`
/// in the area variant), a reference line is a dashed `RuleMark`, and
/// touching the plot (`chartXSelection`) shows a crosshair with the value
/// of every visible series. A `nil` value is a gap: the line breaks, and a
/// lone point between gaps is drawn as a dot. With two or more series the
/// legend shows and hides them; a hidden series leaves the plot and the
/// value range but stays in the data table.
///
/// Accessibility: the chart frame names the chart by its title, gives
/// VoiceOver the data as an accessibility chart descriptor (Audio Graphs and
/// the data table) and offers "View as table", where each row reads the x
/// value and every series. The inspection popover is visual only. Series
/// differ by name and, where set, by dash, never by colour alone.
public struct MonarchLineChart: View {
    private let title: String
    private let xs: MonarchLineChartTokens.XValues
    private let series: [MonarchLineChartSeries]
    private let description: String?
    private let xTitle: String?
    private let valueTitle: String?
    private let referenceLines: [MonarchLineChartReferenceLine]
    private let variant: MonarchLineChartVariant
    private let curve: MonarchLineChartCurve
    private let status: MonarchChartFrameStatus
    private let includesZero: Bool
    private let valueDomain: ClosedRange<Double>?
    private let showsPoints: Bool
    private let isLegendInteractive: Bool
    private let hiddenSeries: Binding<Set<String>>?
    private let activeIndex: Binding<Int?>?
    private let dateFormat: Date.FormatStyle
    private let emptyMessage: String?
    private let footer: String?
    private let height: CGFloat?
    private let labels: MonarchChartFrameLabels
    private let formatValue: ((Double) -> String)?
    private let onRetry: (@MainActor () -> Void)?

    @State private var localHidden: Set<String> = []
    @State private var localActive: Int?
    @Environment(\.locale) private var locale

    /// A line chart over time: points are spaced by their dates, so uneven
    /// dates show.
    ///
    /// - Parameters:
    ///   - title: The name of the chart, already localised.
    ///   - dates: The x values, in order.
    ///   - series: The measures to draw, each with one value per date. Two to five read best.
    ///   - description: One line under the title: the period, the unit or the question it answers.
    ///   - xTitle: The heading of the date column in the data table. Built in: "Date".
    ///   - valueTitle: What the values measure, such as "Sessions", for the data table and VoiceOver.
    ///   - referenceLines: Dashed horizontal lines for targets, baselines or service levels.
    ///   - variant: A line, or a line with a soft fill to the baseline.
    ///   - curve: Straight lines, or a smooth line that never overshoots.
    ///   - status: Whether the body shows the chart, a placeholder, an empty state or an error.
    ///   - includesZero: Whether the value axis keeps zero. Turn it off for values far from zero.
    ///   - valueDomain: Fixes the value axis, such as `0...100` for percentages.
    ///   - showsPoints: Whether every point gets a dot. Lone points between gaps always do.
    ///   - isLegendInteractive: Whether legend entries show and hide their series.
    ///   - hiddenSeries: The keys of hidden series. Kept inside when `nil`.
    ///   - activeIndex: The inspected index, to share one crosshair between charts with the same dates. Kept inside when `nil`.
    ///   - dateFormat: How dates are written in the popover and the data table.
    ///   - emptyMessage: The empty-state message.
    ///   - footer: Notes under the chart: the source, the last update.
    ///   - height: The height of the plot. 240 points by default.
    ///   - labels: Replacements for the built-in strings of the frame.
    ///   - formatValue: Writes a value. The default is a number in the current locale.
    ///   - onRetry: Shows Try again in the error status and runs when it is pressed.
    public init(
        _ title: String,
        dates: [Date],
        series: [MonarchLineChartSeries],
        description: String? = nil,
        xTitle: String? = nil,
        valueTitle: String? = nil,
        referenceLines: [MonarchLineChartReferenceLine] = [],
        variant: MonarchLineChartVariant = .line,
        curve: MonarchLineChartCurve = .linear,
        status: MonarchChartFrameStatus = .ready,
        includesZero: Bool = true,
        valueDomain: ClosedRange<Double>? = nil,
        showsPoints: Bool = false,
        isLegendInteractive: Bool = true,
        hiddenSeries: Binding<Set<String>>? = nil,
        activeIndex: Binding<Int?>? = nil,
        dateFormat: Date.FormatStyle = .dateTime.month(.abbreviated).day(),
        emptyMessage: String? = nil,
        footer: String? = nil,
        height: CGFloat? = nil,
        labels: MonarchChartFrameLabels = MonarchChartFrameLabels(),
        formatValue: ((Double) -> String)? = nil,
        onRetry: (@MainActor () -> Void)? = nil
    ) {
        self.title = title
        self.xs = .dates(dates)
        self.series = series
        self.description = description
        self.xTitle = xTitle
        self.valueTitle = valueTitle
        self.referenceLines = referenceLines
        self.variant = variant
        self.curve = curve
        self.status = status
        self.includesZero = includesZero
        self.valueDomain = valueDomain
        self.showsPoints = showsPoints
        self.isLegendInteractive = isLegendInteractive
        self.hiddenSeries = hiddenSeries
        self.activeIndex = activeIndex
        self.dateFormat = dateFormat
        self.emptyMessage = emptyMessage
        self.footer = footer
        self.height = height
        self.labels = labels
        self.formatValue = formatValue
        self.onRetry = onRetry
    }

    /// A line chart over ordered categories, such as months or releases:
    /// points are spaced evenly.
    ///
    /// - Parameters:
    ///   - title: The name of the chart, already localised.
    ///   - categories: The x values, in order, already localised.
    ///   - series: The measures to draw, each with one value per category. Two to five read best.
    ///   - description: One line under the title: the period, the unit or the question it answers.
    ///   - xTitle: The heading of the category column in the data table. Built in: "Category".
    ///   - valueTitle: What the values measure, such as "Sessions", for the data table and VoiceOver.
    ///   - referenceLines: Dashed horizontal lines for targets, baselines or service levels.
    ///   - variant: A line, or a line with a soft fill to the baseline.
    ///   - curve: Straight lines, or a smooth line that never overshoots.
    ///   - status: Whether the body shows the chart, a placeholder, an empty state or an error.
    ///   - includesZero: Whether the value axis keeps zero. Turn it off for values far from zero.
    ///   - valueDomain: Fixes the value axis, such as `0...100` for percentages.
    ///   - showsPoints: Whether every point gets a dot. Lone points between gaps always do.
    ///   - isLegendInteractive: Whether legend entries show and hide their series.
    ///   - hiddenSeries: The keys of hidden series. Kept inside when `nil`.
    ///   - activeIndex: The inspected index, to share one crosshair between charts with the same categories. Kept inside when `nil`.
    ///   - emptyMessage: The empty-state message.
    ///   - footer: Notes under the chart: the source, the last update.
    ///   - height: The height of the plot. 240 points by default.
    ///   - labels: Replacements for the built-in strings of the frame.
    ///   - formatValue: Writes a value. The default is a number in the current locale.
    ///   - onRetry: Shows Try again in the error status and runs when it is pressed.
    public init(
        _ title: String,
        categories: [String],
        series: [MonarchLineChartSeries],
        description: String? = nil,
        xTitle: String? = nil,
        valueTitle: String? = nil,
        referenceLines: [MonarchLineChartReferenceLine] = [],
        variant: MonarchLineChartVariant = .line,
        curve: MonarchLineChartCurve = .linear,
        status: MonarchChartFrameStatus = .ready,
        includesZero: Bool = true,
        valueDomain: ClosedRange<Double>? = nil,
        showsPoints: Bool = false,
        isLegendInteractive: Bool = true,
        hiddenSeries: Binding<Set<String>>? = nil,
        activeIndex: Binding<Int?>? = nil,
        emptyMessage: String? = nil,
        footer: String? = nil,
        height: CGFloat? = nil,
        labels: MonarchChartFrameLabels = MonarchChartFrameLabels(),
        formatValue: ((Double) -> String)? = nil,
        onRetry: (@MainActor () -> Void)? = nil
    ) {
        self.title = title
        self.xs = .categories(categories)
        self.series = series
        self.description = description
        self.xTitle = xTitle
        self.valueTitle = valueTitle
        self.referenceLines = referenceLines
        self.variant = variant
        self.curve = curve
        self.status = status
        self.includesZero = includesZero
        self.valueDomain = valueDomain
        self.showsPoints = showsPoints
        self.isLegendInteractive = isLegendInteractive
        self.hiddenSeries = hiddenSeries
        self.activeIndex = activeIndex
        self.dateFormat = MonarchLineChartTokens.defaultDateFormat
        self.emptyMessage = emptyMessage
        self.footer = footer
        self.height = height
        self.labels = labels
        self.formatValue = formatValue
        self.onRetry = onRetry
    }

    private var hidden: Binding<Set<String>> { hiddenSeries ?? $localHidden }

    private var active: Int? {
        let value = activeIndex?.wrappedValue ?? localActive
        guard let value, value >= 0, value < xs.count else { return nil }
        return value
    }

    private func setActive(_ value: Int?) {
        if let activeIndex {
            activeIndex.wrappedValue = value
        } else {
            localActive = value
        }
    }

    /// The content of the view.
    public var body: some View {
        let resolved = MonarchLineChartTokens.resolve(series, count: xs.count)
        let xLabels = MonarchLineChartTokens.xLabels(xs, dateFormat: dateFormat, locale: locale)
        let legend = MonarchLineChartTokens.legend(resolved)
        MonarchChartFrame(
            title,
            description: description,
            status: MonarchLineChartTokens.status(status, count: xs.count),
            table: MonarchLineChartTokens.table(
                resolved,
                xLabels: xLabels,
                xTitle: xTitle ?? defaultXTitle,
                valueTitle: valueTitle,
                locale: locale,
                formatValue: formatValue
            ),
            legend: legend,
            hiddenSeries: isLegendInteractive && !legend.isEmpty ? hidden : nil,
            legendMarker: .line,
            emptyMessage: emptyMessage,
            footer: footer,
            height: height,
            labels: labels,
            onRetry: onRetry
        ) {
            plot(resolved.filter { !hidden.wrappedValue.contains($0.key) }, xLabels: xLabels)
                .frame(height: height ?? MonarchChartFrameTokens.defaultHeight)
        }
    }

    private var defaultXTitle: String {
        switch xs {
        case .dates: MonarchLineChartStrings.date
        case .categories: MonarchLineChartStrings.category
        }
    }

    @ViewBuilder private func plot(_ visible: [MonarchLineChartTokens.Series], xLabels: [String]) -> some View {
        let xName = xTitle ?? defaultXTitle
        let valueName = valueTitle ?? MonarchLineChartStrings.value
        let noValue = labels.noValue ?? MonarchChartFrameStrings.noValue
        switch xs {
        case .dates(let dates):
            MonarchLineChartPlot(
                xs: dates,
                xName: xName,
                valueName: valueName,
                series: visible,
                referenceLines: referenceLines,
                variant: variant,
                curve: curve,
                showsPoints: showsPoints,
                includesZero: includesZero,
                valueDomain: valueDomain,
                xLabels: xLabels,
                noValue: noValue,
                active: active,
                formatValue: formatValue,
                indexOf: { MonarchLineChartTokens.nearestIndex(dates, to: $0) },
                setActive: setActive
            )
        case .categories(let categories):
            MonarchLineChartPlot(
                xs: categories,
                xName: xName,
                valueName: valueName,
                series: visible,
                referenceLines: referenceLines,
                variant: variant,
                curve: curve,
                showsPoints: showsPoints,
                includesZero: includesZero,
                valueDomain: valueDomain,
                xLabels: xLabels,
                noValue: noValue,
                active: active,
                formatValue: formatValue,
                indexOf: { categories.firstIndex(of: $0) },
                setActive: setActive
            )
        }
    }
}

#if DEBUG
    #Preview("MonarchLineChart") {
        let months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun"]
        MonarchPreviewMatrix {
            VStack {
                MonarchLineChart(
                    "Revenue",
                    categories: months,
                    series: [
                        MonarchLineChartSeries(
                            "revenue",
                            label: "Revenue",
                            values: [1200, 1450, nil, 1600, 1720, 1810]
                        ),
                        MonarchLineChartSeries(
                            "target",
                            label: "Last year",
                            values: [1000, 1100, 1150, 1200, 1300, 1350],
                            color: .neutral,
                            isDashed: true
                        ),
                    ],
                    description: "Monthly, in dollars",
                    referenceLines: [MonarchLineChartReferenceLine(1500, label: "Target")],
                    activeIndex: .constant(3)
                )
                MonarchLineChart(
                    "Latency",
                    categories: months,
                    series: [MonarchLineChartSeries("p95", label: "p95", values: [210, 220, nil, 260, nil, 235])],
                    variant: .area,
                    curve: .monotone,
                    includesZero: false
                )
            }
        }
    }
#endif
