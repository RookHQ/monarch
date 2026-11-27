import Accessibility
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

private let revenue = MonarchChartTable(
    categoryTitle: "Month",
    categories: ["Jan", "Feb", "Mar", "Apr"],
    series: [
        MonarchChartTableSeries(
            "Revenue",
            values: [1200, 1450, nil, 1600],
            formattedValues: ["$1,200", "$1,450", "", "$1,600"]
        ),
        MonarchChartTableSeries("Costs", values: [900, 950, 1010, -40]),
    ],
    valueTitle: "Dollars"
)

private let legend = [
    MonarchChartLegendItem("revenue", label: "Revenue"),
    MonarchChartLegendItem("costs", label: "Costs"),
]

enum MonarchChartFrameFixtures: ComponentFixtures {
    static let id = "chart-frame"
    static let fixtures = [
        Fixture("ready, with table, legend, actions and footer") {
            MonarchChartFrame(
                "Revenue and costs",
                description: "Last four months, in dollars",
                table: revenue,
                legend: legend,
                footer: "Source: billing. Updated hourly.",
                height: 120,
                content: { MonarchSparkline([1200, 1450, nil, 1600]) },
                actions: { MonarchButton("Export", variant: .secondary, size: .sm) {} }
            )
        },
        Fixture("loading, empty, error with and without retry") {
            VStack {
                MonarchChartFrame("Signups", status: .loading, height: 80) { EmptyView() }
                MonarchChartFrame(
                    "Signups",
                    status: .empty,
                    emptyMessage: "Signups appear here after the first visitor registers."
                ) { EmptyView() }
                MonarchChartFrame("Signups", status: .error, height: 80, onRetry: {}, content: { EmptyView() })
                MonarchChartFrame("Signups", status: .error, height: 80) { EmptyView() }
            }
        },
        Fixture("toggle legend, long title, no table") {
            MonarchChartFrame(
                "Weekly active accounts by acquisition channel across every region",
                legend: legend,
                hiddenSeries: .constant(["costs"]),
                legendMarker: .square,
                height: 80
            ) {
                MonarchSparkline([3, 5, 4, 6], variant: .bar)
            }
        },
        Fixture("data table") {
            MonarchChartFrameTableView(table: revenue, noValue: "No value")
        },
        Fixture("text cells, and the chart's own elements kept") {
            MonarchChartFrame(
                "Risk",
                table: MonarchChartTable(
                    categoryTitle: "Region",
                    categories: ["North", "South"],
                    series: [
                        MonarchChartTableSeries("Score", values: [12, 30]),
                        MonarchChartTableSeries("Band", texts: ["Low", "High"]),
                    ]
                ),
                height: 60,
                contentAccessibility: .contain
            ) {
                MonarchSparkline([12, 30])
            }
        },
    ]
}

@Suite("MonarchChartFrame")
struct MonarchChartFrameTests {
    @Test("The table toggle is offered only with a table, when allowed and while ready")
    func toggleRules() {
        #expect(MonarchChartFrameTokens.showsTableToggle(hasTable: true, isAllowed: true, status: .ready))
        #expect(!MonarchChartFrameTokens.showsTableToggle(hasTable: false, isAllowed: true, status: .ready))
        #expect(!MonarchChartFrameTokens.showsTableToggle(hasTable: true, isAllowed: false, status: .ready))
        for status in [MonarchChartFrameStatus.loading, .empty, .error] {
            #expect(!MonarchChartFrameTokens.showsTableToggle(hasTable: true, isAllowed: true, status: status))
        }
        #expect(MonarchChartFrameTokens.showsTable(isRequested: true, canToggle: true))
        #expect(!MonarchChartFrameTokens.showsTable(isRequested: true, canToggle: false))
    }

    @Test("The legend shows only with entries, while ready and with the chart visible")
    func legendRules() {
        #expect(MonarchChartFrameTokens.showsLegend(entries: 2, status: .ready, showsTable: false))
        #expect(!MonarchChartFrameTokens.showsLegend(entries: 0, status: .ready, showsTable: false))
        #expect(!MonarchChartFrameTokens.showsLegend(entries: 2, status: .loading, showsTable: false))
        #expect(!MonarchChartFrameTokens.showsLegend(entries: 2, status: .ready, showsTable: true))
    }

    @Test("The toggle names the view it switches to, and labels override the built-in text")
    func toggleTitle() {
        let builtIn = MonarchChartFrameLabels()
        #expect(MonarchChartFrameTokens.toggleTitle(showsTable: false, labels: builtIn) == "View as table")
        #expect(MonarchChartFrameTokens.toggleTitle(showsTable: true, labels: builtIn) == "View as chart")
        let custom = MonarchChartFrameLabels(viewTable: "Tabelle")
        #expect(MonarchChartFrameTokens.toggleTitle(showsTable: false, labels: custom) == "Tabelle")
    }

    @Test("Cells use the app's text, else the locale's number, else a gap")
    func cells() {
        #expect(MonarchChartFrameTokens.cellText(revenue.series[0], at: 0) == "$1,200")
        #expect(MonarchChartFrameTokens.cellText(revenue.series[0], at: 2) == nil)
        #expect(MonarchChartFrameTokens.cellText(revenue.series[1], at: 1) == (950.0).formatted())
        #expect(MonarchChartFrameTokens.cellText(revenue.series[1], at: 9) == nil)
    }

    @Test("A row is read as the category, then every series and its value")
    func rowLabel() {
        let label = MonarchChartFrameTokens.rowAccessibilityLabel(revenue, row: 2, noValue: "No value")
        #expect(label == "Mar, Revenue No value, Costs \((1010.0).formatted())")
    }

    @Test("The value axis covers every value and zero")
    func valueRange() {
        #expect(MonarchChartFrameTokens.valueRange(revenue) == -40...1600)
        let empty = MonarchChartTable(categoryTitle: "Day", categories: [], series: [])
        #expect(MonarchChartFrameTokens.valueRange(empty) == 0...1)
    }

    @Test("VoiceOver's chart descriptor has the axes, the series and no gaps")
    @MainActor
    func descriptor() {
        let chart = MonarchChartFrameDescriptor(title: "Revenue", summary: "Last four months", table: revenue)
            .makeChartDescriptor()
        #expect(chart.title == "Revenue")
        #expect(chart.summary == "Last four months")
        #expect(chart.series.count == 2)
        #expect(chart.series[0].name == "Revenue")
        #expect(chart.series[0].dataPoints.count == 3)
        #expect(chart.series[1].dataPoints.count == 4)
        #expect(chart.series[0].dataPoints.first?.label == "$1,200")
        let xAxis = chart.xAxis as? AXCategoricalDataAxisDescriptor
        #expect(xAxis?.categoryOrder == ["Jan", "Feb", "Mar", "Apr"])
        #expect(chart.yAxis?.title == "Dollars")
        #expect(chart.yAxis?.range == -40...1600)
    }

    @Test("The built-in strings resolve")
    func strings() {
        #expect(MonarchChartFrameStrings.viewTable == "View as table")
        #expect(MonarchChartFrameStrings.viewChart == "View as chart")
        #expect(MonarchChartFrameStrings.loading == "Loading chart")
        #expect(MonarchChartFrameStrings.emptyTitle == "No data yet")
        #expect(MonarchChartFrameStrings.error == "This chart didn't load.")
        #expect(MonarchChartFrameStrings.noValue == "No value")
    }

    @Test("Every status keeps at least the chart's height")
    @MainActor
    func keepsHeight() {
        for status in MonarchChartFrameStatus.allCases {
            let size = Host.layout(MonarchChartFrame("Signups", status: status, height: 200) { EmptyView() })
            #expect(size.height >= 200)
        }
    }
}

@Suite("MonarchChartFrame text cells and content")
@MainActor
struct MonarchChartFrameTextCellTests {
    private let table = MonarchChartTable(
        categoryTitle: "Region",
        categories: ["North", "South", "East"],
        series: [
            MonarchChartTableSeries("Score", values: [12, nil, 30]),
            MonarchChartTableSeries("Band", texts: ["Low", nil, "High"]),
        ]
    )

    @Test("Text cells show their text; a missing one is a gap")
    func cells() {
        let band = table.series[1]
        #expect(MonarchChartFrameTokens.cellText(band, at: 0) == "Low")
        #expect(MonarchChartFrameTokens.cellText(band, at: 1) == nil)
        #expect(MonarchChartFrameTokens.cellText(band, at: 2) == "High")
        #expect(MonarchChartFrameTokens.cellText(table.series[0], at: 1) == nil)
        let row = MonarchChartFrameTokens.rowAccessibilityLabel(table, row: 0, noValue: "No value")
        #expect(row == "North, Score 12, Band Low")
    }

    @Test("Only series with numbers go into the audio graph")
    func descriptor() {
        #expect(!MonarchChartFrameTokens.isTextOnly(table.series[0]))
        #expect(MonarchChartFrameTokens.isTextOnly(table.series[1]))
        #expect(!MonarchChartFrameTokens.isTextOnly(MonarchChartTableSeries("Gaps", values: [nil, nil])))
        let descriptor = MonarchChartFrameDescriptor(title: "Risk", summary: nil, table: table).makeChartDescriptor()
        #expect(descriptor.series.map(\.name) == ["Score"])
    }

    @Test("Both content modes render")
    func modes() {
        #expect(MonarchChartFrameContentAccessibility.allCases == [.chart, .contain])
        for mode in MonarchChartFrameContentAccessibility.allCases {
            let frame = MonarchChartFrame("Risk", table: table, height: 40, contentAccessibility: mode) {
                Text("Chart")
            }
            #expect(Host.layout(frame).height > 40)
        }
    }
}
