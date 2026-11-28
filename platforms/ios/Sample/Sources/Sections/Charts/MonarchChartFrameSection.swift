import Charts
import MonarchUI
import SwiftUI

struct MonarchChartFrameSection: CataloguePage {
    static let title = "ChartFrame"

    @Environment(\.monarchTheme) private var theme
    @State private var status = MonarchChartFrameStatus.ready
    @State private var hidden: Set<String> = []
    @State private var period = "6m"

    private let months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun"]
    private let revenue: [Double] = [1200, 1450, 1390, 1600, 1720, 1810]
    private let costs: [Double] = [900, 950, 1010, 980, 1040, 1100]

    private var table: MonarchChartTable {
        MonarchChartTable(
            categoryTitle: "Month",
            categories: months,
            series: [
                MonarchChartTableSeries("Revenue", values: revenue),
                MonarchChartTableSeries("Costs", values: costs),
            ],
            valueTitle: "Dollars"
        )
    }

    var body: some View {
        Picker("Status", selection: $status) {
            Text("Ready").tag(MonarchChartFrameStatus.ready)
            Text("Loading").tag(MonarchChartFrameStatus.loading)
            Text("Empty").tag(MonarchChartFrameStatus.empty)
            Text("Error").tag(MonarchChartFrameStatus.error)
        }
        .pickerStyle(.segmented)
        MonarchChartFrame(
            "Revenue and costs",
            description: "Monthly, in dollars",
            status: status,
            table: table,
            legend: [
                MonarchChartLegendItem("revenue", label: "Revenue"),
                MonarchChartLegendItem("costs", label: "Costs"),
            ],
            hiddenSeries: $hidden,
            emptyMessage: "Revenue appears after the first paid invoice.",
            footer: "Source: billing.",
            onRetry: { status = .ready },
            content: { chart },
            actions: {
                Picker("Period", selection: $period) {
                    Text("6 months").tag("6m")
                    Text("1 year").tag("1y")
                }
                .pickerStyle(.menu)
            }
        )
    }

    private var chart: some View {
        Chart {
            if !hidden.contains("revenue") {
                ForEach(Array(months.enumerated()), id: \.offset) { index, month in
                    LineMark(x: .value("Month", month), y: .value("Revenue", revenue[index]))
                        .foregroundStyle(by: .value("Series", "Revenue"))
                }
            }
            if !hidden.contains("costs") {
                ForEach(Array(months.enumerated()), id: \.offset) { index, month in
                    LineMark(x: .value("Month", month), y: .value("Costs", costs[index]))
                        .foregroundStyle(by: .value("Series", "Costs"))
                }
            }
        }
        .chartForegroundStyleScale(["Revenue": theme.colors.data1, "Costs": theme.colors.data2])
        .chartLegend(.hidden)
    }
}
