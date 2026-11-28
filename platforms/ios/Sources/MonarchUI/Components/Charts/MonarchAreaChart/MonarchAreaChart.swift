import SwiftUI

/// An area chart for volumes over time: several series overlapping,
/// stacked so the top edge is the total, or stacked to 100% to show shares,
/// in a ``MonarchChartFrame``.
///
/// ```swift
/// MonarchAreaChart(
///     "Traffic by platform",
///     dates: days,
///     series: [
///         MonarchLineChartSeries("web", label: "Web", values: web),
///         MonarchLineChartSeries("ios", label: "iOS", values: ios),
///         MonarchLineChartSeries("android", label: "Android", values: android),
///     ],
///     stacking: .stacked
/// )
/// ```
///
/// Built on Swift Charts: each series is an `AreaMark` between its lower and
/// upper edge with a `LineMark` on the upper edge, a reference line is a
/// dashed `RuleMark`, and touching the plot (`chartXSelection`) shows a
/// crosshair with the value of every visible series, top of the stack
/// first, and the total. The stack is computed by Monarch: the first series
/// sits at the bottom, a negative value counts as zero, and a `nil` value is
/// a gap that breaks that series' band. In a 100% stack the axis is shares
/// and the popover gives each value with its share. Series, gaps, colours,
/// the curve and reference lines are the same as ``MonarchLineChart``.
///
/// Accessibility: the chart frame names the chart by its title, gives
/// VoiceOver the data as an accessibility chart descriptor (Audio Graphs and
/// the data table) and offers "View as table", where each row reads the x
/// value, every series and, in a stack, the total. The inspection popover is
/// visual only. Series differ by name in the legend and, where set, by dash,
/// never by colour alone.
public struct MonarchAreaChart: View {
    private let title: String
    private let xs: MonarchLineChartTokens.XValues
    private let series: [MonarchLineChartSeries]
    private let description: String?
    private let xTitle: String?
    private let valueTitle: String?
    private let stacking: MonarchAreaChartStacking
    private let referenceLines: [MonarchLineChartReferenceLine]
    private let curve: MonarchLineChartCurve
    private let status: MonarchChartFrameStatus
    private let includesZero: Bool
    private let valueDomain: ClosedRange<Double>?
    private let isLegendInteractive: Bool
    private let hiddenSeries: Binding<Set<String>>?
    private let activeIndex: Binding<Int?>?
    private let dateFormat: Date.FormatStyle
    private let emptyMessage: String?
    private let footer: String?
    private let height: CGFloat?
    private let labels: MonarchChartFrameLabels
    private let totalLabel: String?
    private let shareLabel: (@Sendable (_ value: String, _ share: String) -> String)?
    private let formatValue: ((Double) -> String)?
    private let onRetry: (@MainActor () -> Void)?

    @State private var localHidden: Set<String> = []
    @State private var localActive: Int?
    @Environment(\.locale) private var locale

    /// An area chart over time: points are spaced by their dates, so uneven
    /// dates show.
    ///
    /// - Parameters:
    ///   - title: The name of the chart, already localised.
    ///   - dates: The x values, in order.
    ///   - series: The measures, bottom of the stack first, each with one value per date.
    ///   - description: One line under the title: the period, the unit or the question it answers.
    ///   - xTitle: The heading of the date column in the data table. Built in: "Date".
    ///   - valueTitle: What the values measure, such as "Visits", for the data table and VoiceOver.
    ///   - stacking: Overlapping areas, a stack whose top is the total, or a stack of shares of 100%.
    ///   - referenceLines: Dashed horizontal lines for targets or baselines. Not drawn in a 100% stack.
    ///   - curve: Straight edges, or smooth edges that never overshoot.
    ///   - status: Whether the body shows the chart, a placeholder, an empty state or an error.
    ///   - includesZero: Whether the value axis keeps zero when the areas overlap. Stacks always do.
    ///   - valueDomain: Fixes the value axis. Ignored in a 100% stack, which is always 0 to 100%.
    ///   - isLegendInteractive: Whether legend entries show and hide their series.
    ///   - hiddenSeries: The keys of hidden series. Kept inside when `nil`.
    ///   - activeIndex: The inspected index, to share one crosshair between charts with the same dates. Kept inside when `nil`.
    ///   - dateFormat: How dates are written in the popover and the data table.
    ///   - emptyMessage: The empty-state message.
    ///   - footer: Notes under the chart: the source, the last update.
    ///   - height: The height of the plot. 240 points by default.
    ///   - labels: Replacements for the built-in strings of the frame.
    ///   - totalLabel: Replaces the built-in "Total" of a stack.
    ///   - shareLabel: Replaces the built-in "value (share)" text of a 100% stack.
    ///   - formatValue: Writes a value. The default is a number in the current locale.
    ///   - onRetry: Shows Try again in the error status and runs when it is pressed.
    public init(
        _ title: String,
        dates: [Date],
        series: [MonarchLineChartSeries],
        description: String? = nil,
        xTitle: String? = nil,
        valueTitle: String? = nil,
        stacking: MonarchAreaChartStacking = .overlap,
        referenceLines: [MonarchLineChartReferenceLine] = [],
        curve: MonarchLineChartCurve = .linear,
        status: MonarchChartFrameStatus = .ready,
        includesZero: Bool = true,
        valueDomain: ClosedRange<Double>? = nil,
        isLegendInteractive: Bool = true,
        hiddenSeries: Binding<Set<String>>? = nil,
        activeIndex: Binding<Int?>? = nil,
        dateFormat: Date.FormatStyle = .dateTime.month(.abbreviated).day(),
        emptyMessage: String? = nil,
        footer: String? = nil,
        height: CGFloat? = nil,
        labels: MonarchChartFrameLabels = MonarchChartFrameLabels(),
        totalLabel: String? = nil,
        shareLabel: (@Sendable (_ value: String, _ share: String) -> String)? = nil,
        formatValue: ((Double) -> String)? = nil,
        onRetry: (@MainActor () -> Void)? = nil
    ) {
        self.title = title
        self.xs = .dates(dates)
        self.series = series
        self.description = description
        self.xTitle = xTitle
        self.valueTitle = valueTitle
        self.stacking = stacking
        self.referenceLines = referenceLines
        self.curve = curve
        self.status = status
        self.includesZero = includesZero
        self.valueDomain = valueDomain
        self.isLegendInteractive = isLegendInteractive
        self.hiddenSeries = hiddenSeries
        self.activeIndex = activeIndex
        self.dateFormat = dateFormat
        self.emptyMessage = emptyMessage
        self.footer = footer
        self.height = height
        self.labels = labels
        self.totalLabel = totalLabel
        self.shareLabel = shareLabel
        self.formatValue = formatValue
        self.onRetry = onRetry
    }

    /// An area chart over ordered categories, such as months or releases:
    /// points are spaced evenly.
    ///
    /// - Parameters:
    ///   - title: The name of the chart, already localised.
    ///   - categories: The x values, in order, already localised.
    ///   - series: The measures, bottom of the stack first, each with one value per category.
    ///   - description: One line under the title: the period, the unit or the question it answers.
    ///   - xTitle: The heading of the category column in the data table. Built in: "Category".
    ///   - valueTitle: What the values measure, such as "Visits", for the data table and VoiceOver.
    ///   - stacking: Overlapping areas, a stack whose top is the total, or a stack of shares of 100%.
    ///   - referenceLines: Dashed horizontal lines for targets or baselines. Not drawn in a 100% stack.
    ///   - curve: Straight edges, or smooth edges that never overshoot.
    ///   - status: Whether the body shows the chart, a placeholder, an empty state or an error.
    ///   - includesZero: Whether the value axis keeps zero when the areas overlap. Stacks always do.
    ///   - valueDomain: Fixes the value axis. Ignored in a 100% stack, which is always 0 to 100%.
    ///   - isLegendInteractive: Whether legend entries show and hide their series.
    ///   - hiddenSeries: The keys of hidden series. Kept inside when `nil`.
    ///   - activeIndex: The inspected index, to share one crosshair between charts with the same categories. Kept inside when `nil`.
    ///   - emptyMessage: The empty-state message.
    ///   - footer: Notes under the chart: the source, the last update.
    ///   - height: The height of the plot. 240 points by default.
    ///   - labels: Replacements for the built-in strings of the frame.
    ///   - totalLabel: Replaces the built-in "Total" of a stack.
    ///   - shareLabel: Replaces the built-in "value (share)" text of a 100% stack.
    ///   - formatValue: Writes a value. The default is a number in the current locale.
    ///   - onRetry: Shows Try again in the error status and runs when it is pressed.
    public init(
        _ title: String,
        categories: [String],
        series: [MonarchLineChartSeries],
        description: String? = nil,
        xTitle: String? = nil,
        valueTitle: String? = nil,
        stacking: MonarchAreaChartStacking = .overlap,
        referenceLines: [MonarchLineChartReferenceLine] = [],
        curve: MonarchLineChartCurve = .linear,
        status: MonarchChartFrameStatus = .ready,
        includesZero: Bool = true,
        valueDomain: ClosedRange<Double>? = nil,
        isLegendInteractive: Bool = true,
        hiddenSeries: Binding<Set<String>>? = nil,
        activeIndex: Binding<Int?>? = nil,
        emptyMessage: String? = nil,
        footer: String? = nil,
        height: CGFloat? = nil,
        labels: MonarchChartFrameLabels = MonarchChartFrameLabels(),
        totalLabel: String? = nil,
        shareLabel: (@Sendable (_ value: String, _ share: String) -> String)? = nil,
        formatValue: ((Double) -> String)? = nil,
        onRetry: (@MainActor () -> Void)? = nil
    ) {
        self.title = title
        self.xs = .categories(categories)
        self.series = series
        self.description = description
        self.xTitle = xTitle
        self.valueTitle = valueTitle
        self.stacking = stacking
        self.referenceLines = referenceLines
        self.curve = curve
        self.status = status
        self.includesZero = includesZero
        self.valueDomain = valueDomain
        self.isLegendInteractive = isLegendInteractive
        self.hiddenSeries = hiddenSeries
        self.activeIndex = activeIndex
        self.dateFormat = MonarchLineChartTokens.defaultDateFormat
        self.emptyMessage = emptyMessage
        self.footer = footer
        self.height = height
        self.labels = labels
        self.totalLabel = totalLabel
        self.shareLabel = shareLabel
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

    private var total: String { totalLabel ?? MonarchAreaChartStrings.total }

    private func share(_ value: String, _ share: String) -> String {
        shareLabel?(value, share) ?? MonarchAreaChartStrings.share(value, share)
    }

    /// The content of the view.
    public var body: some View {
        let resolved = MonarchLineChartTokens.resolve(series, count: xs.count)
        let xLabels = MonarchLineChartTokens.xLabels(xs, dateFormat: dateFormat, locale: locale)
        let legend = MonarchAreaChartTokens.legend(resolved)
        MonarchChartFrame(
            title,
            description: description,
            status: MonarchLineChartTokens.status(status, count: xs.count),
            table: MonarchAreaChartTokens.table(
                resolved,
                xLabels: xLabels,
                xTitle: xTitle ?? defaultXTitle,
                valueTitle: valueTitle,
                stacking: stacking,
                totalLabel: total,
                locale: locale,
                formatValue: formatValue
            ),
            legend: legend,
            hiddenSeries: isLegendInteractive && !legend.isEmpty ? hidden : nil,
            legendMarker: .square,
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
        let stack = MonarchAreaChartTokens.stack(
            visible,
            count: xs.count,
            stacking: stacking,
            baseline: MonarchAreaChartTokens.baseline(
                visible,
                stacking: stacking,
                domain: valueDomain,
                includesZero: includesZero
            )
        )
        let xName = xTitle ?? defaultXTitle
        let valueName = valueTitle ?? MonarchLineChartStrings.value
        let noValue = labels.noValue ?? MonarchChartFrameStrings.noValue
        let rows: (Int) -> [MonarchChartTooltip.Row] = { index in
            MonarchAreaChartTokens.tooltipRows(
                stack,
                at: index,
                stacking: stacking,
                noValue: noValue,
                locale: locale,
                formatValue: formatValue,
                share: share
            )
        }
        let footer: (Int) -> String? = { index in
            MonarchAreaChartTokens.totalText(
                stack,
                at: index,
                stacking: stacking,
                totalLabel: total,
                locale: locale,
                formatValue: formatValue
            )
        }
        let lines = MonarchAreaChartTokens.referenceLines(referenceLines, stacking: stacking)
        let domain = MonarchAreaChartTokens.valueDomain(valueDomain, stacking: stacking)
        let keepsZero = MonarchAreaChartTokens.includesZero(includesZero, stacking: stacking)
        switch xs {
        case .dates(let dates):
            MonarchAreaChartPlot(
                xs: dates,
                xName: xName,
                valueName: valueName,
                stack: stack,
                stacking: stacking,
                referenceLines: lines,
                curve: curve,
                includesZero: keepsZero,
                valueDomain: domain,
                xLabels: xLabels,
                active: active,
                tooltipRows: rows,
                tooltipFooter: footer,
                formatValue: formatValue,
                indexOf: { MonarchLineChartTokens.nearestIndex(dates, to: $0) },
                setActive: setActive
            )
        case .categories(let categories):
            MonarchAreaChartPlot(
                xs: categories,
                xName: xName,
                valueName: valueName,
                stack: stack,
                stacking: stacking,
                referenceLines: lines,
                curve: curve,
                includesZero: keepsZero,
                valueDomain: domain,
                xLabels: xLabels,
                active: active,
                tooltipRows: rows,
                tooltipFooter: footer,
                formatValue: formatValue,
                indexOf: { categories.firstIndex(of: $0) },
                setActive: setActive
            )
        }
    }
}

#if DEBUG
    #Preview("MonarchAreaChart") {
        let months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun"]
        let series = [
            MonarchLineChartSeries("web", label: "Web", values: [420, 460, 445, 480, 610, 640]),
            MonarchLineChartSeries("ios", label: "iOS", values: [210, 230, nil, 240, 300, 320]),
            MonarchLineChartSeries("android", label: "Android", values: [150, 160, 170, 160, 210, 220]),
        ]
        MonarchPreviewMatrix {
            VStack {
                MonarchAreaChart(
                    "Traffic",
                    categories: months,
                    series: series,
                    stacking: .stacked,
                    activeIndex: .constant(4)
                )
                MonarchAreaChart("Share of traffic", categories: months, series: series, stacking: .expand)
                MonarchAreaChart(
                    "Users",
                    categories: months,
                    series: [
                        MonarchLineChartSeries("users", label: "Users", values: [1000, 1085, 1170, 1255, 1340, 1425])
                    ],
                    referenceLines: [MonarchLineChartReferenceLine(1200, label: "Goal")],
                    curve: .monotone
                )
            }
        }
    }
#endif
