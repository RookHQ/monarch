import SwiftUI

/// Stacked bars: how each category's total splits into parts, as values or
/// as shares of 100%, vertical or horizontal.
///
/// ```swift
/// MonarchStackedBarChart(
///     "Tickets by status",
///     categories: ["Jan", "Feb", "Mar"],
///     series: [
///         MonarchBarChartSeries("resolved", label: "Resolved", values: [120, 132, 101]),
///         MonarchBarChartSeries("pending", label: "Pending", values: [22, 18, 30]),
///     ],
///     offset: .expand
/// )
/// ```
///
/// It is ``MonarchBarChart`` with stacking on by default and the 100%
/// offset, so inspection, legend toggles, drill-in, value labels and "View as
/// table" behave the same. With `offset: .expand` each category's values
/// become shares of its total, computed over every series (a hidden series
/// leaves a gap rather than rescaling the others); the value axis runs from
/// 0% to 100%, values are written as percentages and reference lines are not
/// drawn. `onSelect` still receives the category index of the app's data.
///
/// Accessibility: as ``MonarchBarChart``. With the 100% offset the generated
/// summary gives each shown series' average share and its largest share,
/// and the chart descriptor and table carry the shares as percentages.
public struct MonarchStackedBarChart: View {
    private typealias Tokens = MonarchStackedBarChartTokens

    private let title: String
    private let categories: [String]
    private let series: [MonarchBarChartSeries]
    private let description: String?
    private let categoryTitle: String?
    private let valueTitle: String?
    private let summary: String?
    private let orientation: MonarchBarChartOrientation
    private let layout: MonarchBarChartLayout
    private let offset: MonarchStackedBarChartOffset
    private let status: MonarchChartFrameStatus
    private let hiddenSeries: Binding<Set<String>>?
    private let highlightedIndex: Int?
    private let referenceLines: [MonarchBarChartReferenceLine]
    private let valueRange: ClosedRange<Double>?
    private let showsValueLabels: Bool
    private let isLegendToggleable: Bool
    private let isTableToggleShown: Bool
    private let emptyMessage: String?
    private let footer: String?
    private let height: CGFloat?
    private let labels: MonarchStackedBarChartLabels
    private let formatValue: ((Double) -> String)?
    private let onRetry: (@MainActor () -> Void)?
    private let onSelect: (@MainActor (Int) -> Void)?

    @State private var ownHidden: Set<String> = []
    @Environment(\.locale) private var locale

    /// A stacked bar chart.
    ///
    /// - Parameters:
    ///   - title: The name of the chart, already localised.
    ///   - categories: The categories in display order, already formatted.
    ///   - series: The parts, each with one value per category. The first
    ///     series sits on the baseline.
    ///   - description: One line under the title: the period or the unit.
    ///   - categoryTitle: The heading of the category column of the table.
    ///     Built in: "Category".
    ///   - valueTitle: What the values measure, for VoiceOver's value axis.
    ///   - summary: Replaces the generated spoken summary.
    ///   - orientation: Columns, or horizontal bars for long names and rankings.
    ///   - layout: Stacked (the default), or the parts side by side.
    ///   - offset: Values, or shares so every bar is 100% long. Only applies
    ///     when stacked.
    ///   - status: Whether the chart, a placeholder, an empty state or an
    ///     error shows. With no categories or series, ready shows empty.
    ///   - hiddenSeries: The keys of hidden series. Without it the chart
    ///     keeps its own.
    ///   - highlightedIndex: A category to emphasise; the others are dimmed.
    ///   - referenceLines: Dashed lines for targets or averages. Not drawn
    ///     with the 100% offset.
    ///   - valueRange: Fixes the value axis. 0% to 100% with the 100% offset.
    ///   - showsValueLabels: Whether each stack's total (a part's value when
    ///     grouped) is written at its end.
    ///   - isLegendToggleable: Whether legend entries show and hide their series.
    ///   - isTableToggleShown: Whether "View as table" is offered.
    ///   - emptyMessage: The empty-state message.
    ///   - footer: Notes under the chart: the source, the last update.
    ///   - height: The height of the plot, as ``MonarchBarChart``.
    ///   - labels: Replacements for the built-in strings.
    ///   - formatValue: Writes values. Ignored with the 100% offset, which
    ///     writes percentages in the environment's locale.
    ///   - onRetry: Shows Try again in the error status and runs when it is pressed.
    ///   - onSelect: Opens a category: Details in the inspection row, and one
    ///     VoiceOver action per category.
    public init(
        _ title: String,
        categories: [String],
        series: [MonarchBarChartSeries],
        description: String? = nil,
        categoryTitle: String? = nil,
        valueTitle: String? = nil,
        summary: String? = nil,
        orientation: MonarchBarChartOrientation = .vertical,
        layout: MonarchBarChartLayout = .stacked,
        offset: MonarchStackedBarChartOffset = .normal,
        status: MonarchChartFrameStatus = .ready,
        hiddenSeries: Binding<Set<String>>? = nil,
        highlightedIndex: Int? = nil,
        referenceLines: [MonarchBarChartReferenceLine] = [],
        valueRange: ClosedRange<Double>? = nil,
        showsValueLabels: Bool = false,
        isLegendToggleable: Bool = true,
        isTableToggleShown: Bool = true,
        emptyMessage: String? = nil,
        footer: String? = nil,
        height: CGFloat? = nil,
        labels: MonarchStackedBarChartLabels = MonarchStackedBarChartLabels(),
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
        self.offset = offset
        self.status = status
        self.hiddenSeries = hiddenSeries
        self.highlightedIndex = highlightedIndex
        self.referenceLines = referenceLines
        self.valueRange = valueRange
        self.showsValueLabels = showsValueLabels
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

    /// The hidden set: the app's, or the chart's own. Always handed to the
    /// bar chart, so the share summary knows which series are shown.
    private var hidden: Binding<Set<String>> { hiddenSeries ?? $ownHidden }

    /// The content of the view.
    public var body: some View {
        let isExpanded = Tokens.isExpanded(layout: layout, offset: offset)
        let locale = self.locale
        let percent: (Double) -> String = { Tokens.percentText($0, locale: locale) }
        let shares = isExpanded ? Tokens.shares(categories: categories, series: series) : series
        let spoken =
            summary
            ?? (isExpanded
                ? Tokens.shareSummary(
                    categories: categories,
                    shares: shares,
                    hidden: hidden.wrappedValue,
                    format: percent,
                    seriesFormat: labels.shareSeries ?? MonarchStackedBarChartStrings.shareSeries,
                    noData: labels.bar.noData ?? MonarchBarChartStrings.noData
                )
                : nil)
        MonarchBarChart(
            title,
            categories: categories,
            series: shares,
            description: description,
            categoryTitle: categoryTitle,
            valueTitle: valueTitle,
            summary: spoken,
            orientation: orientation,
            layout: layout,
            status: status,
            hiddenSeries: hidden,
            highlightedIndex: highlightedIndex,
            referenceLines: isExpanded ? [] : referenceLines,
            valueRange: isExpanded ? Tokens.expandedRange : valueRange,
            showsValueLabels: showsValueLabels,
            isLegendToggleable: isLegendToggleable,
            isTableToggleShown: isTableToggleShown,
            emptyMessage: emptyMessage,
            footer: footer,
            height: height,
            labels: labels.bar,
            formatValue: isExpanded ? percent : formatValue,
            onRetry: onRetry,
            onSelect: onSelect
        )
    }
}

#if DEBUG
    #Preview("MonarchStackedBarChart") {
        let months = ["Jan", "Feb", "Mar", "Apr"]
        let series = [
            MonarchBarChartSeries("resolved", label: "Resolved", values: [120, 132, 101, 134]),
            MonarchBarChartSeries("pending", label: "Pending", values: [22, 18, 30, 25]),
            MonarchBarChartSeries("escalated", label: "Escalated", values: [8, 6, nil, 9]),
        ]
        MonarchPreviewMatrix {
            VStack {
                MonarchStackedBarChart("Tickets by status", categories: months, series: series, showsValueLabels: true)
                MonarchStackedBarChart(
                    "Tickets by status",
                    categories: months,
                    series: series,
                    orientation: .horizontal,
                    offset: .expand
                )
                MonarchStackedBarChart("Tickets by status", categories: months, series: series, layout: .grouped)
            }
        }
    }
#endif
