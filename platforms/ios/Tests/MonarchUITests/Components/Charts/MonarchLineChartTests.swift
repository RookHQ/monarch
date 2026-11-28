import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchLineChartFixtures: ComponentFixtures {
    static let id = "line-chart"

    /// Fourteen days at noon UTC, fixed so the fixtures never depend on the clock.
    static let days: [Date] = (0..<14).map { Date(timeIntervalSince1970: 1_772_366_400 + Double($0) * 86_400) }
    static let sessions: [Double?] = [820, 932, 901, 934, 1290, 1330, 1320, 1180, 1250, 1400, 1380, 1460, 1510, 1490]
    static let previous: [Double?] = sessions.map { $0.map { ($0 * 0.82).rounded() } }
    static let latency: [Double?] = [210, 220, nil, nil, 260, 240, 410, 380, 250, 230, 225, nil, 240, nil]
    static let utc = Date.FormatStyle(date: .abbreviated, time: .omitted, timeZone: TimeZone(identifier: "UTC") ?? .gmt)

    static let fixtures = [
        Fixture("two series, dashed comparison, target and inspection") {
            MonarchLineChart(
                "Sessions",
                dates: days,
                series: [
                    MonarchLineChartSeries("current", label: "This period", values: sessions),
                    MonarchLineChartSeries(
                        "previous",
                        label: "Previous period",
                        values: previous,
                        color: .neutral,
                        isDashed: true
                    ),
                ],
                description: "Last 14 days against the previous 14",
                referenceLines: [MonarchLineChartReferenceLine(1_200, label: "Target")],
                activeIndex: .constant(5),
                dateFormat: utc
            )
        },
        Fixture("area with gaps, monotone, zero left out, custom format") {
            MonarchLineChart(
                "p95 latency",
                dates: days,
                series: [MonarchLineChartSeries("p95", label: "p95", values: latency, color: .data3)],
                variant: .area,
                curve: .monotone,
                includesZero: false,
                height: 180,
                formatValue: { "\(Int($0)) ms" }
            )
        },
        Fixture("categories, fixed domain, points, a hidden series and six series") {
            MonarchLineChart(
                "Conversion",
                categories: ["Jan", "Feb", "Mar", "Apr", "May", "Jun"],
                series: (0..<6).map { index in
                    MonarchLineChartSeries(
                        "s\(index)",
                        label: "A long series name number \(index + 1)",
                        values: [10, 20, 30, 40, 50, 60].map { Double($0 + index * 5) }
                    )
                },
                valueDomain: 0...100,
                showsPoints: true,
                hiddenSeries: .constant(["s1"]),
                activeIndex: .constant(2)
            )
        },
        Fixture("loading, error, empty and a static legend") {
            VStack {
                MonarchLineChart("Sessions", dates: days, series: [], status: .loading, height: 80)
                MonarchLineChart("Sessions", dates: days, series: [], status: .error, height: 80, onRetry: {})
                MonarchLineChart("Sessions", dates: [], series: [], emptyMessage: "Data appears after the first visit.")
                MonarchLineChart(
                    "Sign-ups",
                    categories: ["Q1", "Q2"],
                    series: [
                        MonarchLineChartSeries("a", label: "Web", values: [1, 2]),
                        MonarchLineChartSeries("b", label: "iOS", values: [2, 1]),
                    ],
                    isLegendInteractive: false,
                    height: 120
                )
            }
        },
        Fixture("inspection popover") {
            MonarchLineChartTooltip(
                title: "Mar 4",
                rows: MonarchLineChartTokens.tooltipRows(
                    MonarchLineChartTokens.resolve(
                        [
                            MonarchLineChartSeries("a", label: "Sessions", values: [1_204]),
                            MonarchLineChartSeries("b", label: "Previous", values: [nil], isDashed: true),
                        ],
                        count: 1
                    ),
                    at: 0,
                    noValue: "No value",
                    locale: Locale(identifier: "en_US"),
                    formatValue: nil
                )
            )
        },
    ]
}

@Suite("MonarchLineChart")
struct MonarchLineChartTests {
    private let english = Locale(identifier: "en_US")

    @Test("Series get one value per x: missing and non-finite values are gaps, extras are dropped")
    func resolve() {
        let resolved = MonarchLineChartTokens.resolve(
            [
                MonarchLineChartSeries("a", label: "A", values: [1, .nan, 3, 4, 5]),
                MonarchLineChartSeries("b", label: "B", values: [1], color: .data4, isDashed: true),
            ],
            count: 4
        )
        #expect(resolved[0].values == [1, nil, 3, 4])
        #expect(resolved[1].values == [1, nil, nil, nil])
        #expect(resolved.map(\.color) == [.data1, .data4])
        #expect(resolved[1].isDashed)
        #expect(resolved[0].role == \MonarchColorTokens.data1)
        #expect(
            MonarchLineChartTokens.resolve([MonarchLineChartSeries("a", label: "A", values: [1])], count: -1)[0].values
                .isEmpty
        )
    }

    @Test("Colour slots follow the position and never cycle: the sixth series is neutral")
    func colors() {
        let series = (0..<7).map { MonarchLineChartSeries("\($0)", label: "\($0)", values: [1]) }
        let colors = MonarchLineChartTokens.resolve(series, count: 1).map(\.color)
        #expect(colors == [.data1, .data2, .data3, .data4, .data5, .neutral, .neutral])
    }

    @Test("Gaps split a line into runs, and lone values get a point")
    func runs() {
        let values: [Double?] = [1, 2, nil, 4, nil, nil, 7, 8, 9, nil, 11]
        #expect(MonarchLineChartTokens.runs(values) == [[0, 1], [3], [6, 7, 8], [10]])
        #expect(MonarchLineChartTokens.pointIndices(values, showsPoints: false) == [3, 10])
        #expect(MonarchLineChartTokens.pointIndices(values, showsPoints: true) == [0, 1, 3, 6, 7, 8, 10])
        #expect(MonarchLineChartTokens.runs([nil, nil]).isEmpty)
        #expect(MonarchLineChartTokens.runs([]).isEmpty)
    }

    @Test("A touch on a date finds the nearest date")
    func nearest() {
        let days = MonarchLineChartFixtures.days
        #expect(MonarchLineChartTokens.nearestIndex(days, to: days[3]) == 3)
        #expect(MonarchLineChartTokens.nearestIndex(days, to: days[3].addingTimeInterval(30_000)) == 3)
        #expect(MonarchLineChartTokens.nearestIndex(days, to: days[3].addingTimeInterval(60_000)) == 4)
        #expect(MonarchLineChartTokens.nearestIndex(days, to: .distantFuture) == 13)
        #expect(MonarchLineChartTokens.nearestIndex([], to: days[0]) == nil)
    }

    @Test("The area fills to zero, to a fixed domain's zero, or to the lowest value")
    func baseline() {
        #expect(MonarchLineChartTokens.areaBaseline([200, 300], domain: nil, includesZero: true) == 0)
        #expect(MonarchLineChartTokens.areaBaseline([200, 300], domain: nil, includesZero: false) == 200)
        #expect(MonarchLineChartTokens.areaBaseline([200, 300], domain: 100...400, includesZero: true) == 100)
        #expect(MonarchLineChartTokens.areaBaseline([-5, 5], domain: -10...10, includesZero: false) == 0)
        #expect(MonarchLineChartTokens.areaBaseline([-50, -20], domain: -100 ... -10, includesZero: true) == -10)
        #expect(MonarchLineChartTokens.areaBaseline([], domain: nil, includesZero: false) == 0)
    }

    @Test("A ready chart with no x values shows the empty state")
    func status() {
        #expect(MonarchLineChartTokens.status(.ready, count: 0) == .empty)
        #expect(MonarchLineChartTokens.status(.ready, count: 3) == .ready)
        #expect(MonarchLineChartTokens.status(.loading, count: 0) == .loading)
        #expect(MonarchLineChartTokens.status(.error, count: 0) == .error)
    }

    @Test("Dates are written with the given format and locale")
    func xLabels() {
        let labels = MonarchLineChartTokens.xLabels(
            .dates(Array(MonarchLineChartFixtures.days.prefix(2))),
            dateFormat: MonarchLineChartFixtures.utc,
            locale: english
        )
        #expect(labels == ["Mar 1, 2026", "Mar 2, 2026"])
        #expect(
            MonarchLineChartTokens.xLabels(
                .categories(["Q1"]),
                dateFormat: MonarchLineChartFixtures.utc,
                locale: english
            )
                == ["Q1"]
        )
    }

    @Test("The data table keeps every series and the chart's own text for each value")
    func table() {
        let resolved = MonarchLineChartTokens.resolve(
            [MonarchLineChartSeries("a", label: "Sessions", values: [1_204, nil])],
            count: 2
        )
        let table = MonarchLineChartTokens.table(
            resolved,
            xLabels: ["Mar 1", "Mar 2"],
            xTitle: "Day",
            valueTitle: "Visits",
            locale: english,
            formatValue: nil
        )
        #expect(table.categoryTitle == "Day")
        #expect(table.categories == ["Mar 1", "Mar 2"])
        #expect(table.valueTitle == "Visits")
        #expect(table.series.first?.values == [1_204, nil])
        #expect(table.series.first?.formattedValues == ["1,204", ""])
        #expect(
            MonarchChartFrameTokens.rowAccessibilityLabel(table, row: 1, noValue: "No value")
                == "Mar 2, Sessions No value"
        )
    }

    @Test("A single series has no legend; several have one entry each with their dash")
    func legend() {
        let one = MonarchLineChartTokens.resolve([MonarchLineChartSeries("a", label: "A", values: [])], count: 0)
        #expect(MonarchLineChartTokens.legend(one).isEmpty)
        let two = MonarchLineChartTokens.resolve(
            [
                MonarchLineChartSeries("a", label: "A", values: []),
                MonarchLineChartSeries("b", label: "B", values: [], color: .neutral, isDashed: true),
            ],
            count: 0
        )
        let legend = MonarchLineChartTokens.legend(two)
        #expect(legend.map(\.key) == ["a", "b"])
        #expect(legend.map(\.color) == [.data1, .neutral])
        #expect(legend.map(\.isDashed) == [false, true])
    }

    @Test("The popover lists every visible series, with No value for a gap")
    func tooltip() {
        let resolved = MonarchLineChartTokens.resolve(
            [
                MonarchLineChartSeries("a", label: "Sessions", values: [1_204]),
                MonarchLineChartSeries("b", label: "Previous", values: [nil]),
            ],
            count: 1
        )
        let rows = MonarchLineChartTokens.tooltipRows(
            resolved,
            at: 0,
            noValue: "No value",
            locale: english,
            formatValue: { "\(Int($0)) visits" }
        )
        #expect(rows.map(\.value) == ["1204 visits", "No value"])
        #expect(rows.map(\.label) == ["Sessions", "Previous"])
        #expect(
            MonarchLineChartTokens.tooltipRows(resolved, at: 9, noValue: "-", locale: english, formatValue: nil).map(
                \.value
            ) == ["-", "-"]
        )
    }

    @Test("A reference line is written as its label and value")
    func reference() {
        #expect(
            MonarchLineChartTokens.referenceText(
                MonarchLineChartReferenceLine(1_200, label: "Target"),
                locale: english,
                formatValue: nil
            ) == "Target 1.2K"
        )
        #expect(
            MonarchLineChartTokens.referenceText(
                MonarchLineChartReferenceLine(99.9),
                locale: english,
                formatValue: { "\($0)%" }
            )
                == "99.9%"
        )
    }

    @Test("The built-in strings come from the component's strings table")
    func strings() {
        #expect(MonarchLineChartStrings.date == "Date")
        #expect(MonarchLineChartStrings.category == "Category")
        #expect(MonarchLineChartStrings.value == "Value")
    }
}
