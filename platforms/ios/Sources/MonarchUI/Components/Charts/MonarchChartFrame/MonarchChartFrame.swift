import SwiftUI

/// The card every chart sits in: a title, a description, controls, a
/// legend, loading, empty and error states, and the chart's data as a table.
///
/// ```swift
/// MonarchChartFrame(
///     "Revenue",
///     description: "Last 6 months, in dollars",
///     table: revenueTable,
///     legend: [MonarchChartLegendItem("revenue", label: "Revenue")]
/// ) {
///     Chart(points) { LineMark(x: .value("Month", $0.month), y: .value("Revenue", $0.value)) }
/// }
/// ```
///
/// The frame draws no chart itself: put a Swift Charts `Chart`, a
/// ``MonarchSparkline`` or any custom drawing in it. Every status keeps the
/// chart's height, so the card does not jump when data arrives. With a
/// `table`, "View as table" swaps the chart for its values, and VoiceOver is
/// given the same data as an accessibility chart descriptor (Audio Graphs).
///
/// Accessibility: the title is a heading. With a table, the chart is one
/// element named by the title, with the chart descriptor, unless
/// `contentAccessibility` is `.contain`, which keeps the chart's own
/// elements (a legend with values, a selected bar) reachable; each table row is
/// read as the category followed by every series and its value. Loading is
/// spoken as "Loading chart". The empty and error states are the Monarch
/// components, with their own behaviour.
public struct MonarchChartFrame<Content: View, Actions: View>: View {
    private let title: String
    private let description: String?
    private let status: MonarchChartFrameStatus
    private let table: MonarchChartTable?
    private let legend: [MonarchChartLegendItem]
    private let hiddenSeries: Binding<Set<String>>?
    private let legendMarker: MonarchChartLegendMarker
    private let emptyMessage: String?
    private let footer: String?
    private let height: CGFloat
    private let isTableToggleShown: Bool
    private let keepsOneVisible: Bool
    private let contentAccessibility: MonarchChartFrameContentAccessibility
    private let labels: MonarchChartFrameLabels
    private let onRetry: (@MainActor () -> Void)?
    private let content: Content
    private let actions: Actions

    @State private var isTableRequested = false
    @Environment(\.monarchTheme) private var theme
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    /// A chart frame with controls in the header.
    ///
    /// - Parameters:
    ///   - title: The name of the chart, already localised.
    ///   - description: One line under the title: the period, the unit or the question it answers.
    ///   - status: Whether the body shows the chart, a placeholder, an empty state or an error.
    ///   - table: The chart's data, for "View as table" and VoiceOver.
    ///   - legend: Legend entries, shown under the header while the chart is visible.
    ///   - hiddenSeries: The keys of hidden series. Given, the legend entries are toggles.
    ///   - legendMarker: The mark beside each legend label.
    ///   - emptyMessage: The empty-state message, such as what will appear and when.
    ///   - footer: Notes under the chart: the source, the last update, a footnote.
    ///   - height: The height kept for the body in every status. 240 points by default.
    ///   - isTableToggleShown: Whether "View as table" is offered when there is a table.
    ///   - keepsOneVisible: Whether the legend keeps the last visible series from being hidden.
    ///   - contentAccessibility: Whether the chart is one element with the table, or keeps its own elements.
    ///   - labels: Replacements for the built-in strings.
    ///   - onRetry: Shows Try again in the error status and runs when it is pressed.
    ///   - content: The chart.
    ///   - actions: Controls in the header, such as a period picker.
    public init(
        _ title: String,
        description: String? = nil,
        status: MonarchChartFrameStatus = .ready,
        table: MonarchChartTable? = nil,
        legend: [MonarchChartLegendItem] = [],
        hiddenSeries: Binding<Set<String>>? = nil,
        legendMarker: MonarchChartLegendMarker = .line,
        emptyMessage: String? = nil,
        footer: String? = nil,
        height: CGFloat? = nil,
        isTableToggleShown: Bool = true,
        keepsOneVisible: Bool = true,
        contentAccessibility: MonarchChartFrameContentAccessibility = .chart,
        labels: MonarchChartFrameLabels = MonarchChartFrameLabels(),
        onRetry: (@MainActor () -> Void)? = nil,
        @ViewBuilder content: () -> Content,
        @ViewBuilder actions: () -> Actions
    ) {
        self.title = title
        self.description = description
        self.status = status
        self.table = table
        self.legend = legend
        self.hiddenSeries = hiddenSeries
        self.legendMarker = legendMarker
        self.emptyMessage = emptyMessage
        self.footer = footer
        self.height = height ?? MonarchChartFrameTokens.defaultHeight
        self.isTableToggleShown = isTableToggleShown
        self.keepsOneVisible = keepsOneVisible
        self.contentAccessibility = contentAccessibility
        self.labels = labels
        self.onRetry = onRetry
        self.content = content()
        self.actions = actions()
    }

    /// The content of the view.
    public var body: some View {
        let canToggle = MonarchChartFrameTokens.showsTableToggle(
            hasTable: table != nil,
            isAllowed: isTableToggleShown,
            status: status
        )
        let showsTable = MonarchChartFrameTokens.showsTable(isRequested: isTableRequested, canToggle: canToggle)
        MonarchCard {
            VStack(alignment: .leading, spacing: MonarchChartFrameTokens.sectionSpacing) {
                header(canToggle: canToggle, showsTable: showsTable)
                if MonarchChartFrameTokens.showsLegend(entries: legend.count, status: status, showsTable: showsTable) {
                    legendView
                }
                // A stack, so the height is kept even when the chart is empty.
                ZStack(alignment: .topLeading) {
                    chartBody(showsTable: showsTable)
                }
                .frame(maxWidth: .infinity, minHeight: height, alignment: .topLeading)
                if let footer, !footer.isEmpty {
                    Text(footer)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors.textMuted)
                }
            }
        }
    }

    private func header(canToggle: Bool, showsTable: Bool) -> some View {
        // As the web: the title on the left and the actions (View as table) on the right, the title wrapping
        // beside them. At the accessibility text sizes the actions go under the title.
        let layout =
            dynamicTypeSize.isAccessibilitySize
            ? AnyLayout(VStackLayout(alignment: .leading, spacing: MonarchChartFrameTokens.actionSpacing))
            : AnyLayout(HStackLayout(alignment: .top, spacing: MonarchChartFrameTokens.actionSpacing))
        return layout {
            heading
                .frame(maxWidth: .infinity, alignment: .leading)
            actionRow(canToggle: canToggle, showsTable: showsTable)
        }
    }

    private var heading: some View {
        VStack(alignment: .leading, spacing: MonarchChartFrameTokens.headingSpacing) {
            Text(title)
                .monarchTextStyle(.label, weight: .semibold)
                .foregroundStyle(theme.colors.textDefault)
                .accessibilityAddTraits(.isHeader)
            if let description, !description.isEmpty {
                Text(description)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
            }
        }
    }

    private func actionRow(canToggle: Bool, showsTable: Bool) -> some View {
        // The controls stack at the right, as the web wraps them: the app's actions, then View as table under them.
        VStack(alignment: .trailing, spacing: MonarchChartFrameTokens.actionSpacing) {
            actions
            if canToggle {
                MonarchButton(
                    MonarchChartFrameTokens.toggleTitle(showsTable: showsTable, labels: labels),
                    variant: .ghost,
                    size: .sm
                ) {
                    isTableRequested.toggle()
                }
                .fixedSize()
            }
        }
        .fixedSize(horizontal: true, vertical: false)
        .layoutPriority(1)
    }

    @ViewBuilder private var legendView: some View {
        if let hiddenSeries {
            MonarchChartLegend(
                legend,
                hiddenSeries: hiddenSeries,
                marker: legendMarker,
                keepsOneVisible: keepsOneVisible,
                legendLabel: labels.legend
            )
        } else {
            MonarchChartLegend(legend, marker: legendMarker, legendLabel: labels.legend)
        }
    }

    @ViewBuilder private func chartBody(showsTable: Bool) -> some View {
        switch status {
        case .loading:
            MonarchShapes.control
                .fill(theme.colors.bgMuted)
                .frame(height: height)
                .accessibilityElement()
                .accessibilityLabel(labels.loading ?? MonarchChartFrameStrings.loading)
                .accessibilityAddTraits(.updatesFrequently)
        case .empty:
            MonarchEmptyState(
                labels.emptyTitle ?? MonarchChartFrameStrings.emptyTitle,
                message: emptyMessage,
                systemImage: MonarchChartFrameTokens.emptySymbol,
                size: .sm
            )
            .frame(maxWidth: .infinity)
        case .error:
            errorView
        case .ready:
            if showsTable, let table {
                MonarchChartFrameTableView(table: table, noValue: labels.noValue ?? MonarchChartFrameStrings.noValue)
            } else if let table, contentAccessibility == .chart {
                content
                    .accessibilityElement(children: .ignore)
                    .accessibilityLabel(title)
                    .accessibilityChartDescriptor(
                        MonarchChartFrameDescriptor(title: title, summary: description, table: table)
                    )
            } else {
                content
            }
        }
    }

    @ViewBuilder private var errorView: some View {
        let message = labels.error ?? MonarchChartFrameStrings.error
        if let onRetry {
            MonarchInlineErrorRetry(message, onRetry: onRetry)
        } else {
            HStack(alignment: .firstTextBaseline, spacing: MonarchChartFrameTokens.errorIconSpacing) {
                MonarchIcon(systemName: MonarchChartFrameTokens.errorSymbol, size: .sm)
                Text(message).monarchTextStyle(.body)
            }
            .foregroundStyle(theme.colors.dangerStrong)
            .accessibilityElement(children: .combine)
        }
    }
}

extension MonarchChartFrame where Actions == EmptyView {
    /// A chart frame without controls in the header.
    ///
    /// - Parameters:
    ///   - title: The name of the chart, already localised.
    ///   - description: One line under the title: the period, the unit or the question it answers.
    ///   - status: Whether the body shows the chart, a placeholder, an empty state or an error.
    ///   - table: The chart's data, for "View as table" and VoiceOver.
    ///   - legend: Legend entries, shown under the header while the chart is visible.
    ///   - hiddenSeries: The keys of hidden series. Given, the legend entries are toggles.
    ///   - legendMarker: The mark beside each legend label.
    ///   - emptyMessage: The empty-state message, such as what will appear and when.
    ///   - footer: Notes under the chart: the source, the last update, a footnote.
    ///   - height: The height kept for the body in every status. 240 points by default.
    ///   - isTableToggleShown: Whether "View as table" is offered when there is a table.
    ///   - keepsOneVisible: Whether the legend keeps the last visible series from being hidden.
    ///   - contentAccessibility: Whether the chart is one element with the table, or keeps its own elements.
    ///   - labels: Replacements for the built-in strings.
    ///   - onRetry: Shows Try again in the error status and runs when it is pressed.
    ///   - content: The chart.
    public init(
        _ title: String,
        description: String? = nil,
        status: MonarchChartFrameStatus = .ready,
        table: MonarchChartTable? = nil,
        legend: [MonarchChartLegendItem] = [],
        hiddenSeries: Binding<Set<String>>? = nil,
        legendMarker: MonarchChartLegendMarker = .line,
        emptyMessage: String? = nil,
        footer: String? = nil,
        height: CGFloat? = nil,
        isTableToggleShown: Bool = true,
        keepsOneVisible: Bool = true,
        contentAccessibility: MonarchChartFrameContentAccessibility = .chart,
        labels: MonarchChartFrameLabels = MonarchChartFrameLabels(),
        onRetry: (@MainActor () -> Void)? = nil,
        @ViewBuilder content: () -> Content
    ) {
        self.init(
            title,
            description: description,
            status: status,
            table: table,
            legend: legend,
            hiddenSeries: hiddenSeries,
            legendMarker: legendMarker,
            emptyMessage: emptyMessage,
            footer: footer,
            height: height,
            isTableToggleShown: isTableToggleShown,
            keepsOneVisible: keepsOneVisible,
            contentAccessibility: contentAccessibility,
            labels: labels,
            onRetry: onRetry,
            content: content,
            actions: { EmptyView() }
        )
    }
}

#if DEBUG
    #Preview("MonarchChartFrame") {
        let table = MonarchChartTable(
            categoryTitle: "Month",
            categories: ["Jan", "Feb", "Mar"],
            series: [MonarchChartTableSeries("Revenue", values: [1200, 1450, 1390])]
        )
        MonarchPreviewMatrix {
            VStack {
                MonarchChartFrame("Revenue", description: "Last 3 months", table: table, height: 80) {
                    MonarchSparkline([1200, 1450, 1390])
                }
                ForEach([MonarchChartFrameStatus.loading, .empty, .error], id: \.self) { status in
                    MonarchChartFrame("Revenue", status: status, height: 80) { EmptyView() }
                }
            }
        }
    }
#endif
