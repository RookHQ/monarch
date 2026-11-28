import Accessibility
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

private let months = ["Jan", "Feb", "Mar", "Apr"]

private let channels = [
    MonarchBarChartSeries("web", label: "Web", values: [1200, 1450, 1390, 1600]),
    MonarchBarChartSeries("ios", label: "iOS", values: [800, nil, 1010, -40]),
]

private func dollars(_ value: Double) -> String { "$\(Int(value))" }

private func visible(_ hidden: Set<String> = []) -> [(series: MonarchBarChartSeries, color: MonarchChartColor)] {
    MonarchBarChartTokens.visible(channels, hidden: hidden)
}

enum MonarchBarChartFixtures: ComponentFixtures {
    static let id = "bar-chart"
    static let fixtures = [
        Fixture("grouped columns, legend, highlight, reference line") {
            MonarchBarChart(
                "Revenue by channel",
                categories: months,
                series: channels,
                description: "Last four months",
                highlightedIndex: 2,
                referenceLines: [MonarchBarChartReferenceLine(1500, label: "Target")],
                footer: "Source: billing.",
                height: 160,
                formatValue: dollars
            )
        },
        Fixture("stacked columns with value labels and a fixed range") {
            MonarchBarChart(
                "Revenue by channel",
                categories: months,
                series: channels,
                layout: .stacked,
                valueRange: -500...3500,
                showsValueLabels: true,
                height: 160
            )
        },
        Fixture("horizontal bars, long categories, grouped and stacked") {
            VStack {
                MonarchBarChart(
                    "Top pages",
                    categories: ["/pricing/enterprise-plans-and-volume-discounts", "/docs", "/blog"],
                    series: [MonarchBarChartSeries("views", label: "Views", values: [4210, 2980, 1120])],
                    orientation: .horizontal,
                    showsValueLabels: true
                )
                MonarchBarChart(
                    "Revenue by channel",
                    categories: months,
                    series: channels,
                    orientation: .horizontal,
                    layout: .stacked,
                    showsValueLabels: true
                )
            }
        },
        Fixture("seven series, the last two neutral; one hidden; static legend") {
            MonarchBarChart(
                "Signups by source",
                categories: ["Q1", "Q2"],
                series: (1...7).map {
                    MonarchBarChartSeries("s\($0)", label: "Source \($0)", values: [Double($0), 8])
                },
                hiddenSeries: .constant(["s2"]),
                isLegendToggleable: false,
                height: 120
            )
        },
        Fixture("loading, empty, error with retry, no table toggle") {
            VStack {
                MonarchBarChart("Revenue", categories: months, series: channels, status: .loading, height: 80)
                MonarchBarChart(
                    "Revenue",
                    categories: [],
                    series: channels,
                    emptyMessage: "Revenue appears after the first invoice."
                )
                MonarchBarChart(
                    "Revenue",
                    categories: months,
                    series: channels,
                    status: .error,
                    height: 80,
                    onRetry: {}
                )
                MonarchBarChart(
                    "Revenue",
                    categories: months,
                    series: [channels[0]],
                    isTableToggleShown: false,
                    height: 80,
                    onSelect: { _ in }
                )
            }
        },
        Fixture("custom labels and data frame") {
            MonarchChartDataFrame(
                title: "Revenue",
                description: nil,
                status: .ready,
                table: MonarchBarChartTokens.table(
                    categoryTitle: "Month",
                    categories: months,
                    series: channels,
                    layout: .stacked,
                    valueTitle: nil,
                    format: dollars,
                    total: "Total"
                ),
                legend: [],
                hiddenSeries: nil,
                legendMarker: .square,
                emptyMessage: nil,
                footer: nil,
                height: 80,
                isTableToggleShown: true,
                labels: MonarchChartFrameLabels(viewTable: "Tabelle"),
                onRetry: nil
            ) {
                MonarchSparkline([1, 3, 2])
            }
        },
    ]
}

@Suite("MonarchBarChart")
struct MonarchBarChartTests {
    @Test("Gaps and values that are not finite draw nothing")
    func values() {
        let series = MonarchBarChartSeries("a", label: "A", values: [1, nil, .nan, .infinity])
        #expect(MonarchBarChartTokens.value(series, at: 0) == 1)
        #expect(MonarchBarChartTokens.value(series, at: 1) == nil)
        #expect(MonarchBarChartTokens.value(series, at: 2) == nil)
        #expect(MonarchBarChartTokens.value(series, at: 3) == nil)
        #expect(MonarchBarChartTokens.value(series, at: 9) == nil)
    }

    @Test("Colours follow the position among every series, so hiding one does not move the others")
    func colours() {
        let shown = visible(["web"])
        #expect(shown.count == 1)
        #expect(shown[0].series.key == "ios")
        #expect(shown[0].color == .data2)
        let pinned = MonarchBarChartSeries("x", label: "X", values: [], color: .data4)
        #expect(MonarchBarChartTokens.color(pinned, index: 0) == .data4)
        #expect(MonarchBarChartTokens.color(channels[0], index: 5) == .neutral)
        #expect(MonarchBarChartTokens.role(.data1) == \MonarchColorTokens.data1)
        #expect(MonarchBarChartTokens.role(.neutral) == \MonarchColorTokens.textSubtle)
    }

    @Test("Grouped bars start at zero and skip gaps")
    func grouped() {
        let segments = MonarchBarChartTokens.segments(categories: months, visible: visible(), layout: .grouped)
        #expect(segments.count == 7)
        #expect(segments.allSatisfy { $0.start == 0 && $0.end == $0.value })
        #expect(!segments.contains { $0.categoryIndex == 1 && $0.seriesKey == "ios" })
        #expect(segments.first { $0.categoryIndex == 3 && $0.seriesKey == "ios" }?.end == -40)
    }

    @Test("Stacked bars stack positive values up and negative values down")
    func stacked() {
        let segments = MonarchBarChartTokens.segments(categories: months, visible: visible(), layout: .stacked)
        let march = segments.filter { $0.categoryIndex == 2 }
        #expect(march.map(\.start) == [0, 1390])
        #expect(march.map(\.end) == [1390, 2400])
        let april = segments.filter { $0.categoryIndex == 3 }
        #expect(april.map(\.start) == [0, 0])
        #expect(april.map(\.end) == [1600, -40])
    }

    @Test("A segment separator pulls the ends of stacked parts in, and leaves a bar alone when off")
    func segmentSeparator() {
        let segments = MonarchBarChartTokens.segments(categories: months, visible: visible(), layout: .stacked)
        let extent = MonarchBarChartTokens.extent(segments)
        let march = segments.filter { $0.categoryIndex == 2 }
        #expect(MonarchBarChartTokens.separated(march[1], isOn: false, extent: extent) == march[1])
        let set = MonarchBarChartTokens.separated(march[1], isOn: true, extent: extent)
        #expect(set.start > march[1].start && set.end < march[1].end)
        #expect(set.id == march[1].id)
        // The gap between two stacked parts is the same on both sides of their shared edge.
        let lower = MonarchBarChartTokens.separated(march[0], isOn: true, extent: extent)
        #expect(set.start - lower.end > 0)
        // A negative part moves the other way: it stays between its own ends.
        let april = segments.filter { $0.categoryIndex == 3 }[1]
        let negative = MonarchBarChartTokens.separated(april, isOn: true, extent: extent)
        #expect(negative.end > april.end && negative.start < april.start)
        // A very thin part keeps most of its length.
        let thin = MonarchBarChartTokens.separated(
            .init(categoryIndex: 0, category: "A", seriesKey: "s", color: .data1, value: 1, start: 0, end: 1),
            isOn: true,
            extent: 1_000
        )
        #expect(thin.end - thin.start >= 0.75)
    }

    @Test("Value labels: every bar when grouped, one total per stack at its end")
    func valueLabels() {
        let grouped = MonarchBarChartTokens.segments(categories: months, visible: visible(), layout: .grouped)
        #expect(MonarchBarChartTokens.valueLabels(grouped, layout: .grouped).count == grouped.count)
        let stacked = MonarchBarChartTokens.segments(categories: months, visible: visible(), layout: .stacked)
        let labels = MonarchBarChartTokens.valueLabels(stacked, layout: .stacked)
        #expect(labels.count == 4)
        #expect(labels["2-ios"] == 2400)
        #expect(labels["3-web"] == 1560)
        #expect(labels["1-web"] == 1450)
        let negative = MonarchBarChartTokens.segments(
            categories: ["Q1"],
            visible: [
                (series: MonarchBarChartSeries("a", label: "A", values: [10]), color: .data1),
                (series: MonarchBarChartSeries("b", label: "B", values: [-30]), color: .data2),
            ],
            layout: .stacked
        )
        #expect(MonarchBarChartTokens.valueLabels(negative, layout: .stacked) == ["0-b": -20])
    }

    @Test("Ready with no categories or no series is empty; other statuses are kept")
    func status() {
        #expect(MonarchBarChartTokens.resolvedStatus(.ready, categories: [], series: channels) == .empty)
        #expect(MonarchBarChartTokens.resolvedStatus(.ready, categories: months, series: []) == .empty)
        #expect(MonarchBarChartTokens.resolvedStatus(.ready, categories: months, series: channels) == .ready)
        #expect(MonarchBarChartTokens.resolvedStatus(.loading, categories: [], series: []) == .loading)
        #expect(MonarchBarChartTokens.resolvedStatus(.error, categories: [], series: []) == .error)
    }

    @Test("A legend for two or more series; dimming only with a highlight")
    func legendAndHighlight() {
        #expect(!MonarchBarChartTokens.showsLegend(seriesCount: 1))
        #expect(MonarchBarChartTokens.showsLegend(seriesCount: 2))
        #expect(!MonarchBarChartTokens.isDimmed(0, highlighted: nil))
        #expect(!MonarchBarChartTokens.isDimmed(2, highlighted: 2))
        #expect(MonarchBarChartTokens.isDimmed(1, highlighted: 2))
    }

    @Test("Height: given, 240 for columns, 32 per category and at least 120 for bars")
    func height() {
        #expect(MonarchBarChartTokens.height(100, orientation: .horizontal, count: 50) == 100)
        #expect(MonarchBarChartTokens.height(nil, orientation: .vertical, count: 50) == 240)
        #expect(MonarchBarChartTokens.height(nil, orientation: .horizontal, count: 2) == 120)
        #expect(MonarchBarChartTokens.height(nil, orientation: .horizontal, count: 10) == 352)
    }

    @Test("The inspected category is found by name")
    func inspectedIndex() {
        #expect(MonarchBarChartTokens.index(of: "Mar", in: months) == 2)
        #expect(MonarchBarChartTokens.index(of: "Dec", in: months) == nil)
        #expect(MonarchBarChartTokens.index(of: nil, in: months) == nil)
    }

    @Test("The inspection reads the category, each series, gaps and the stack total")
    func inspection() {
        let grouped = MonarchBarChartTokens.inspection(
            at: 1,
            categories: months,
            visible: visible(),
            layout: .grouped,
            format: dollars,
            noValue: "No value",
            total: "Total"
        )
        #expect(grouped == "Feb: Web $1450, iOS No value")
        let stacked = MonarchBarChartTokens.inspection(
            at: 2,
            categories: months,
            visible: visible(),
            layout: .stacked,
            format: dollars,
            noValue: "No value",
            total: "Total"
        )
        #expect(stacked == "Mar: Web $1390, iOS $1010, Total $2400")
        let single = MonarchBarChartTokens.inspection(
            at: 2,
            categories: months,
            visible: visible(["ios"]),
            layout: .stacked,
            format: dollars,
            noValue: "No value",
            total: "Total"
        )
        #expect(single == "Mar: Web $1390")
    }

    @Test("The summary names the range, each series' total and highest category, and reference lines")
    func summary() {
        let text = MonarchBarChartTokens.summary(
            categories: months,
            visible: visible(),
            referenceLines: [MonarchBarChartReferenceLine(1500, label: "Target")],
            format: dollars,
            noData: "No data"
        )
        #expect(
            text == "4 points from Jan to Apr. Web: total $5640, highest Apr at $1600. "
                + "iOS: total $1770, highest Mar at $1010. Target $1500"
        )
        let one = MonarchBarChartTokens.summary(
            categories: ["Jan"],
            visible: [(series: MonarchBarChartSeries("a", label: "A", values: [nil]), color: .data1)],
            referenceLines: [],
            format: dollars,
            noData: "No data"
        )
        #expect(one == "1 point, Jan. A: No data")
        let none = MonarchBarChartTokens.summary(
            categories: [],
            visible: visible(),
            referenceLines: [],
            format: nil,
            noData: "No data"
        )
        #expect(none == "No data")
    }

    @Test("The table lists every series, hidden ones too, and a total column when stacked")
    func table() {
        let grouped = MonarchBarChartTokens.table(
            categoryTitle: "Month",
            categories: months,
            series: channels,
            layout: .grouped,
            valueTitle: "Dollars",
            format: dollars,
            total: "Total"
        )
        #expect(grouped.categoryTitle == "Month")
        #expect(grouped.series.map(\.name) == ["Web", "iOS"])
        #expect(grouped.series[1].values == [800, nil, 1010, -40])
        #expect(grouped.series[0].formattedValues?.first == "$1200")
        #expect(grouped.valueTitle == "Dollars")
        let stacked = MonarchBarChartTokens.table(
            categoryTitle: "Month",
            categories: months,
            series: channels,
            layout: .stacked,
            valueTitle: nil,
            format: dollars,
            total: "Total"
        )
        #expect(stacked.series.map(\.name) == ["Web", "iOS", "Total"])
        #expect(stacked.series[2].values == [2000, 1450, 2400, 1560])
        let single = MonarchBarChartTokens.table(
            categoryTitle: "Month",
            categories: months,
            series: [channels[0]],
            layout: .stacked,
            valueTitle: nil,
            format: nil,
            total: "Total"
        )
        #expect(single.series.count == 1)
    }

    @Test("VoiceOver's chart descriptor carries the summary, the categories and each series")
    @MainActor
    func descriptor() {
        let table = MonarchBarChartTokens.table(
            categoryTitle: "Month",
            categories: months,
            series: channels,
            layout: .grouped,
            valueTitle: "Dollars",
            format: dollars,
            total: "Total"
        )
        let chart = MonarchChartFrameDescriptor(title: "Revenue", summary: "4 points", table: table)
            .makeChartDescriptor()
        #expect(chart.summary == "4 points")
        #expect(chart.series.map(\.name) == ["Web", "iOS"])
        #expect(chart.series[1].dataPoints.count == 3)
        #expect((chart.xAxis as? AXCategoricalDataAxisDescriptor)?.categoryOrder == months)
    }

    @Test("The VoiceOver action names the category it opens")
    func actionName() {
        #expect(MonarchBarChartTokens.detailsActionName("Details", category: "Mar") == "Details, Mar")
    }

    @Test("The built-in strings resolve")
    func strings() {
        #expect(MonarchBarChartStrings.category == "Category")
        #expect(MonarchBarChartStrings.total == "Total")
        #expect(MonarchBarChartStrings.noData == "No data")
        #expect(MonarchBarChartStrings.details == "Details")
        #expect(MonarchBarChartStrings.clearSelection == "Clear")
        #expect(MonarchBarChartStrings.onePoint("Jan") == "1 point, Jan")
        #expect(MonarchBarChartStrings.points(count: "3", first: "Jan", last: "Mar") == "3 points from Jan to Mar")
        #expect(
            MonarchBarChartStrings.series("Web", total: "$9", highest: "Mar", value: "$5")
                == "Web: total $9, highest Mar at $5"
        )
    }

    #if os(iOS)
        @Test("The chart keeps at least its plot height in every status")
        @MainActor
        func keepsHeight() {
            for status in MonarchChartFrameStatus.allCases {
                let size = Host.layout(
                    MonarchBarChart("Revenue", categories: months, series: channels, status: status, height: 200)
                )
                #expect(size.height >= 200)
            }
        }
    #endif
}
