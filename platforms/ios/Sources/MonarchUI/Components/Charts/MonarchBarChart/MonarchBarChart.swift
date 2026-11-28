import Charts
import MonarchTokens
import SwiftUI

/// A bar or column chart: one or more series per category, grouped or
/// stacked, vertical or horizontal, inside a ``MonarchChartFrame``.
///
/// ```swift
/// MonarchBarChart(
///     "Revenue by month",
///     categories: ["Jan", "Feb", "Mar"],
///     series: [
///         MonarchBarChartSeries("web", label: "Web", values: [1200, 1450, 1390]),
///         MonarchBarChartSeries("ios", label: "iOS", values: [800, 920, 1010]),
///     ],
///     layout: .stacked,
///     formatValue: { $0.formatted(.currency(code: "USD")) }
/// )
/// ```
///
/// Drawn with Swift Charts (`BarMark`, `RuleMark`). Colours are the theme's
/// data slots in series order (the sixth series and later are neutral).
/// With two or more series a square-marker legend names them, and its
/// entries show and hide each series. Touching or dragging across the plot
/// inspects a category (`chartXSelection`, or `chartYSelection` for
/// horizontal bars): its band is shaded and a row under the plot reads its
/// values, with Details when `onSelect` is given.
///
/// Accessibility: the plot is one element named by the title. Its value is
/// a generated summary ("3 points from Jan to Mar. Web: total …"), or
/// `summary`. It carries an accessibility chart descriptor with every
/// category and series (Audio Graphs and the chart data rotor). With
/// `onSelect`, each category is a VoiceOver action ("Details, Mar"). "View
/// as table" shows every value as text. The inspection row is ordinary text
/// with its buttons.
public struct MonarchBarChart: View {
    private typealias Tokens = MonarchBarChartTokens

    private let title: String
    private let categories: [String]
    private let series: [MonarchBarChartSeries]
    private let description: String?
    private let categoryTitle: String?
    private let valueTitle: String?
    private let summary: String?
    private let orientation: MonarchBarChartOrientation
    private let layout: MonarchBarChartLayout
    private let status: MonarchChartFrameStatus
    private let hiddenSeries: Binding<Set<String>>?
    private let highlightedIndex: Int?
    private let referenceLines: [MonarchBarChartReferenceLine]
    private let valueRange: ClosedRange<Double>?
    private let showsValueLabels: Bool
    private let segmentSeparator: Bool
    private let isLegendToggleable: Bool
    private let isTableToggleShown: Bool
    private let emptyMessage: String?
    private let footer: String?
    private let height: CGFloat?
    private let labels: MonarchBarChartLabels
    private let formatValue: ((Double) -> String)?
    private let onRetry: (@MainActor () -> Void)?
    private let onSelect: (@MainActor (Int) -> Void)?

    @State private var ownHidden: Set<String> = []
    @State private var selection: String?
    @State private var inspected: String?
    @Environment(\.monarchTheme) private var theme

    /// A bar chart.
    ///
    /// - Parameters:
    ///   - title: The name of the chart, already localised.
    ///   - categories: The categories in display order, already formatted.
    ///   - series: The measures to draw, each with one value per category.
    ///   - description: One line under the title: the period or the unit.
    ///   - categoryTitle: The heading of the category column of the table.
    ///     Built in: "Category".
    ///   - valueTitle: What the values measure, for VoiceOver's value axis.
    ///   - summary: Replaces the generated spoken summary.
    ///   - orientation: Columns, or horizontal bars for long names and rankings.
    ///   - layout: With several series, side by side or stacked.
    ///   - status: Whether the chart, a placeholder, an empty state or an
    ///     error shows. With no categories or series, ready shows empty.
    ///   - hiddenSeries: The keys of hidden series. Without it the chart
    ///     keeps its own.
    ///   - highlightedIndex: A category to emphasise; the others are dimmed.
    ///   - referenceLines: Dashed lines across the bars for targets or averages.
    ///   - valueRange: Fixes the value axis. It fits the values and zero when `nil`.
    ///   - showsValueLabels: Whether each bar's value (a stack's total) is
    ///     written at its end.
    ///   - segmentSeparator: Whether the parts of a stacked bar are set apart by a thin gap, so neighbouring
    ///     series stay distinct for people who cannot tell their colours apart. Stacked layout only.
    ///   - isLegendToggleable: Whether legend entries show and hide their series.
    ///   - isTableToggleShown: Whether "View as table" is offered.
    ///   - emptyMessage: The empty-state message.
    ///   - footer: Notes under the chart: the source, the last update.
    ///   - height: The height of the plot. 240 points for columns; 32 points
    ///     per category, at least 120, for bars.
    ///   - labels: Replacements for the built-in strings.
    ///   - formatValue: Writes values in the axis, labels, table and summary.
    ///     The default is a number in the current locale.
    ///   - onRetry: Shows Try again in the error status and runs when it is pressed.
    ///   - onSelect: Opens a category, such as a filtered list: Details in the
    ///     inspection row, and one VoiceOver action per category.
    public init(
        _ title: String,
        categories: [String],
        series: [MonarchBarChartSeries],
        description: String? = nil,
        categoryTitle: String? = nil,
        valueTitle: String? = nil,
        summary: String? = nil,
        orientation: MonarchBarChartOrientation = .vertical,
        layout: MonarchBarChartLayout = .grouped,
        status: MonarchChartFrameStatus = .ready,
        hiddenSeries: Binding<Set<String>>? = nil,
        highlightedIndex: Int? = nil,
        referenceLines: [MonarchBarChartReferenceLine] = [],
        valueRange: ClosedRange<Double>? = nil,
        showsValueLabels: Bool = false,
        segmentSeparator: Bool = false,
        isLegendToggleable: Bool = true,
        isTableToggleShown: Bool = true,
        emptyMessage: String? = nil,
        footer: String? = nil,
        height: CGFloat? = nil,
        labels: MonarchBarChartLabels = MonarchBarChartLabels(),
        formatValue: ((Double) -> String)? = nil,
        onRetry: (@MainActor () -> Void)? = nil,
        onSelect: (@MainActor (Int) -> Void)? = nil
    ) {
        self.title = title
        self.categories = categories
        self.series = series
        self.description = description
        self.categoryTitle = categoryTitle
        self.valueTitle = valueTitle
        self.summary = summary
        self.orientation = orientation
        self.layout = layout
        self.status = status
        self.hiddenSeries = hiddenSeries
        self.highlightedIndex = highlightedIndex
        self.referenceLines = referenceLines
        self.valueRange = valueRange
        self.showsValueLabels = showsValueLabels
        self.segmentSeparator = segmentSeparator
        self.isLegendToggleable = isLegendToggleable
        self.isTableToggleShown = isTableToggleShown
        self.emptyMessage = emptyMessage
        self.footer = footer
        self.height = height
        self.labels = labels
        self.formatValue = formatValue
        self.onRetry = onRetry
        self.onSelect = onSelect
    }

    private var hidden: Set<String> { hiddenSeries?.wrappedValue ?? ownHidden }

    private var legendBinding: Binding<Set<String>>? {
        guard isLegendToggleable else { return nil }
        return hiddenSeries ?? $ownHidden
    }

    private var legend: [MonarchChartLegendItem] {
        guard Tokens.showsLegend(seriesCount: series.count) else { return [] }
        return series.enumerated().map { index, series in
            MonarchChartLegendItem(series.key, label: series.label, color: Tokens.color(series, index: index))
        }
    }

    private var totalLabel: String { labels.total ?? MonarchBarChartStrings.total }

    /// The content of the view.
    public var body: some View {
        let visible = Tokens.visible(series, hidden: hidden)
        let plotHeight = Tokens.height(height, orientation: orientation, count: categories.count)
        let table = Tokens.table(
            categoryTitle: categoryTitle ?? labels.category ?? MonarchBarChartStrings.category,
            categories: categories,
            series: series,
            layout: layout,
            valueTitle: valueTitle,
            format: formatValue,
            total: totalLabel
        )
        let spoken =
            summary
            ?? Tokens.summary(
                categories: categories,
                visible: visible,
                referenceLines: referenceLines,
                format: formatValue,
                noData: labels.noData ?? MonarchBarChartStrings.noData
            )
        MonarchChartDataFrame(
            title: title,
            description: description,
            status: Tokens.resolvedStatus(status, categories: categories, series: series),
            table: table,
            legend: legend,
            hiddenSeries: legendBinding,
            legendMarker: .square,
            emptyMessage: emptyMessage,
            footer: footer,
            height: plotHeight,
            isTableToggleShown: isTableToggleShown,
            labels: labels.frame,
            onRetry: onRetry
        ) {
            VStack(alignment: .leading, spacing: Tokens.detailSpacing) {
                plot(visible: visible)
                    .frame(height: plotHeight)
                    .modifier(MonarchChartAccessibility(title: title, summary: spoken, table: table))
                    .accessibilityActions {
                        if let onSelect {
                            ForEach(Array(categories.enumerated()), id: \.offset) { index, category in
                                Button(
                                    Tokens.detailsActionName(
                                        labels.details ?? MonarchBarChartStrings.details,
                                        category: category
                                    )
                                ) {
                                    onSelect(index)
                                }
                            }
                        }
                    }
                    .onChange(of: selection) { _, category in
                        if let category { inspected = category }
                    }
                if let index = Tokens.index(of: inspected, in: categories) {
                    inspectionRow(index: index, visible: visible)
                }
            }
        }
    }

    // MARK: Plot

    @ViewBuilder
    private func plot(visible: [(series: MonarchBarChartSeries, color: MonarchChartColor)]) -> some View {
        let segments = Tokens.segments(categories: categories, visible: visible, layout: layout)
        let valueLabels = showsValueLabels ? Tokens.valueLabels(segments, layout: layout) : [:]
        let extent = Tokens.extent(segments)
        let chart = Chart {
            if let inspected, categories.contains(inspected) {
                band(inspected)
            }
            ForEach(segments) { segment in
                bar(
                    Tokens.separated(segment, isOn: segmentSeparator && layout == .stacked, extent: extent),
                    label: valueLabels[segment.id]
                )
            }
            zeroLine
            ForEach(Array(referenceLines.enumerated()), id: \.offset) { _, line in
                reference(line)
            }
        }
        .chartLegend(.hidden)
        switch orientation {
        case .vertical:
            scaled(
                chart
                    .chartXScale(domain: categories)
                    .chartXAxis { categoryAxis }
                    .chartYAxis { verticalValueAxis }
                    .chartXSelection(value: $selection)
            )
        case .horizontal:
            scaled(
                chart
                    .chartYScale(domain: categories)
                    .chartYAxis { categoryAxis }
                    .chartXAxis { valueAxis }
                    .chartYSelection(value: $selection)
            )
        }
    }

    /// Fixes the value axis when the app gives a range.
    @ViewBuilder
    private func scaled(_ chart: some View) -> some View {
        if let valueRange {
            switch orientation {
            case .vertical: chart.chartYScale(domain: valueRange)
            case .horizontal: chart.chartXScale(domain: valueRange)
            }
        } else {
            chart
        }
    }

    @ChartContentBuilder
    private func bar(_ segment: Tokens.Segment, label: Double?) -> some ChartContent {
        let color = theme.colors.soft(Tokens.role(segment.color), by: Tokens.softening)
        let opacity = Tokens.isDimmed(segment.categoryIndex, highlighted: highlightedIndex) ? Tokens.dimmedOpacity : 1
        let group = layout == .grouped ? segment.seriesKey : ""
        let radius = layout == .grouped ? Tokens.barRadius : 0
        switch orientation {
        case .vertical:
            BarMark(
                x: .value("Category", segment.category),
                yStart: .value("Value", segment.start),
                yEnd: .value("Value", segment.end),
                width: .ratio(Tokens.bandFill)
            )
            .position(by: .value("Series", group))
            .foregroundStyle(color)
            .cornerRadius(radius)
            .opacity(opacity)
            .annotation(position: segment.end >= segment.start ? .top : .bottom, spacing: Tokens.valueLabelSpacing) {
                valueLabel(label)
            }
        case .horizontal:
            BarMark(
                xStart: .value("Value", segment.start),
                xEnd: .value("Value", segment.end),
                y: .value("Category", segment.category),
                height: .ratio(Tokens.bandFill)
            )
            .position(by: .value("Series", group))
            .foregroundStyle(color)
            .cornerRadius(radius)
            .opacity(opacity)
            .annotation(
                position: segment.end >= segment.start ? .trailing : .leading,
                spacing: Tokens.valueLabelSpacing
            ) {
                valueLabel(label)
            }
        }
    }

    @ViewBuilder
    private func valueLabel(_ value: Double?) -> some View {
        if let value {
            Text(Tokens.text(value, format: formatValue))
                .monarchTextStyle(Tokens.labelStyle)
                .monospacedDigit()
                .foregroundStyle(theme.colors[role: Tokens.labelRole])
        }
    }

    @ChartContentBuilder
    private func band(_ category: String) -> some ChartContent {
        switch orientation {
        case .vertical:
            RectangleMark(x: .value("Category", category), width: .ratio(1))
                .foregroundStyle(theme.colors[role: Tokens.bandRole])
        case .horizontal:
            RectangleMark(y: .value("Category", category), height: .ratio(1))
                .foregroundStyle(theme.colors[role: Tokens.bandRole])
        }
    }

    @ChartContentBuilder
    private var zeroLine: some ChartContent {
        switch orientation {
        case .vertical:
            RuleMark(y: .value("Value", 0.0))
                .foregroundStyle(theme.colors.borderStrong)
                .lineStyle(StrokeStyle(lineWidth: Tokens.referenceWidth))
        case .horizontal:
            RuleMark(x: .value("Value", 0.0))
                .foregroundStyle(theme.colors.borderStrong)
                .lineStyle(StrokeStyle(lineWidth: Tokens.referenceWidth))
        }
    }

    @ChartContentBuilder
    private func reference(_ line: MonarchBarChartReferenceLine) -> some ChartContent {
        let style = StrokeStyle(lineWidth: Tokens.referenceWidth, dash: Tokens.referenceDash)
        let color = theme.colors[role: Tokens.referenceRole]
        switch orientation {
        case .vertical:
            RuleMark(y: .value("Value", line.value))
                .foregroundStyle(color)
                .lineStyle(style)
                .annotation(position: .top, alignment: .leading, spacing: MonarchTokens.Space.space1) {
                    referenceLabel(line.label)
                }
        case .horizontal:
            RuleMark(x: .value("Value", line.value))
                .foregroundStyle(color)
                .lineStyle(style)
                .annotation(position: .top, alignment: .leading, spacing: MonarchTokens.Space.space1) {
                    referenceLabel(line.label)
                }
        }
    }

    @ViewBuilder
    private func referenceLabel(_ label: String?) -> some View {
        if let label, !label.isEmpty {
            Text(label)
                .monarchTextStyle(Tokens.labelStyle)
                .foregroundStyle(theme.colors[role: Tokens.labelRole])
        }
    }

    private var categoryAxis: some AxisContent {
        AxisMarks { _ in
            AxisValueLabel()
                .foregroundStyle(theme.colors.textMuted)
        }
    }

    /// The value axis of columns: on the leading side, as on the web.
    private var verticalValueAxis: some AxisContent {
        AxisMarks(position: .leading) { value in
            AxisGridLine()
                .foregroundStyle(theme.colors.borderSubtle)
            AxisValueLabel {
                if let number = value.as(Double.self) {
                    Text(Tokens.text(number, format: formatValue))
                }
            }
            .foregroundStyle(theme.colors.textMuted)
        }
    }

    private var valueAxis: some AxisContent {
        AxisMarks { value in
            AxisGridLine()
                .foregroundStyle(theme.colors.borderSubtle)
            AxisValueLabel {
                if let number = value.as(Double.self) {
                    Text(Tokens.text(number, format: formatValue))
                }
            }
            .foregroundStyle(theme.colors.textMuted)
        }
    }

    // MARK: Inspection

    private func inspectionRow(
        index: Int,
        visible: [(series: MonarchBarChartSeries, color: MonarchChartColor)]
    ) -> some View {
        let text = Tokens.inspection(
            at: index,
            categories: categories,
            visible: visible,
            layout: layout,
            format: formatValue,
            noValue: labels.frame.noValue ?? MonarchChartFrameStrings.noValue,
            total: totalLabel
        )
        return ViewThatFits(in: .horizontal) {
            HStack(alignment: .firstTextBaseline, spacing: Tokens.detailSpacing) {
                inspectionText(text)
                Spacer(minLength: 0)
                inspectionButtons(index: index)
            }
            VStack(alignment: .leading, spacing: Tokens.detailSpacing) {
                inspectionText(text)
                inspectionButtons(index: index)
            }
        }
    }

    private func inspectionText(_ text: String) -> some View {
        Text(text)
            .monarchTextStyle(Tokens.labelStyle)
            .monospacedDigit()
            .foregroundStyle(theme.colors.textDefault)
            .fixedSize(horizontal: false, vertical: true)
    }

    private func inspectionButtons(index: Int) -> some View {
        HStack(spacing: Tokens.detailSpacing) {
            if let onSelect {
                MonarchButton(labels.details ?? MonarchBarChartStrings.details, variant: .secondary, size: .sm) {
                    onSelect(index)
                }
            }
            MonarchButton(labels.clearSelection ?? MonarchBarChartStrings.clearSelection, variant: .ghost, size: .sm) {
                inspected = nil
                selection = nil
            }
        }
    }
}

#if DEBUG
    #Preview("MonarchBarChart") {
        let months = ["Jan", "Feb", "Mar", "Apr"]
        let series = [
            MonarchBarChartSeries("web", label: "Web", values: [1200, 1450, 1390, 1600]),
            MonarchBarChartSeries("ios", label: "iOS", values: [800, 920, nil, 1010]),
        ]
        MonarchPreviewMatrix {
            VStack {
                MonarchBarChart(
                    "Revenue",
                    categories: months,
                    series: series,
                    highlightedIndex: 3,
                    referenceLines: [MonarchBarChartReferenceLine(1500, label: "Target")],
                    height: 160
                )
                MonarchBarChart(
                    "Revenue",
                    categories: months,
                    series: series,
                    orientation: .horizontal,
                    layout: .stacked,
                    showsValueLabels: true
                )
                MonarchBarChart("Revenue", categories: [], series: [])
            }
        }
    }
#endif
