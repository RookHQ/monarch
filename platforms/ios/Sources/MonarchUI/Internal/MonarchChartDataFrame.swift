import SwiftUI

/// A ``MonarchChartFrame`` for a chart that Monarch draws itself, with
/// "View as table" and an accessibility element whose value is the chart's
/// spoken summary.
///
/// `MonarchChartFrame` with a `table` names the chart element by its title
/// and gives the chart descriptor the visible description as its summary.
/// Monarch's own charts generate a summary ("6 points from Jan to Jun.
/// Revenue: total …") and keep parts of the chart (a legend with values, an
/// inspected category) readable on their own, so this frame owns the table
/// toggle and lets the chart apply ``MonarchChartAccessibility`` to the plot
/// only. Shared by ``MonarchBarChart``, ``MonarchDonutChart`` and
/// ``MonarchCalendarHeatmap``.
struct MonarchChartDataFrame<Content: View>: View {
    let title: String
    let description: String?
    let status: MonarchChartFrameStatus
    let table: MonarchChartTable
    let legend: [MonarchChartLegendItem]
    let hiddenSeries: Binding<Set<String>>?
    let legendMarker: MonarchChartLegendMarker
    let emptyMessage: String?
    let footer: String?
    let height: CGFloat?
    let isTableToggleShown: Bool
    let labels: MonarchChartFrameLabels
    let onRetry: (@MainActor () -> Void)?
    @ViewBuilder let content: () -> Content

    @State private var isTableRequested = false

    var body: some View {
        let canToggle = MonarchChartFrameTokens.showsTableToggle(
            hasTable: true,
            isAllowed: isTableToggleShown,
            status: status
        )
        let showsTable = MonarchChartFrameTokens.showsTable(isRequested: isTableRequested, canToggle: canToggle)
        MonarchChartFrame(
            title,
            description: description,
            status: status,
            table: nil,
            legend: showsTable ? [] : legend,
            hiddenSeries: hiddenSeries,
            legendMarker: legendMarker,
            emptyMessage: emptyMessage,
            footer: footer,
            height: height,
            isTableToggleShown: false,
            labels: labels,
            onRetry: onRetry,
            content: {
                if showsTable {
                    MonarchChartFrameTableView(
                        table: table,
                        noValue: labels.noValue ?? MonarchChartFrameStrings.noValue
                    )
                } else {
                    content()
                }
            },
            actions: {
                if canToggle {
                    MonarchButton(
                        MonarchChartFrameTokens.toggleTitle(showsTable: showsTable, labels: labels),
                        variant: .ghost,
                        size: .sm
                    ) {
                        isTableRequested.toggle()
                    }
                }
            }
        )
    }
}

/// Makes a plot one accessibility element: named by the chart title, with
/// the summary as its value and the data as an accessibility chart
/// descriptor (Audio Graphs and the chart data rotor).
struct MonarchChartAccessibility: ViewModifier {
    let title: String
    let summary: String
    let table: MonarchChartTable

    func body(content: Content) -> some View {
        content
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(title)
            .accessibilityValue(summary)
            .accessibilityChartDescriptor(MonarchChartFrameDescriptor(title: title, summary: summary, table: table))
    }
}
