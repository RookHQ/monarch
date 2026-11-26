import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchAreaChartFixtures: ComponentFixtures {
    static let id = "area-chart"

    /// Fourteen days at noon UTC, fixed so the fixtures never depend on the clock.
    static let days: [Date] = (0..<14).map { Date(timeIntervalSince1970: 1_772_366_400 + Double($0) * 86_400) }
    static let web: [Double?] = [420, 460, 445, 480, 610, 640, 600, 520, 560, 610, 600, 640, 690, 670]
    static let ios: [Double?] = [210, 230, 220, 240, 300, 320, 310, 260, nil, 300, 310, 330, 350, 340]
    static let android: [Double?] = [150, 160, 170, 160, 210, 220, 230, 190, 200, 220, 230, 240, 250, 260]
    static let utc = Date.FormatStyle(date: .abbreviated, time: .omitted, timeZone: TimeZone(identifier: "UTC") ?? .gmt)

    static var platforms: [MonarchLineChartSeries] {
        [
            MonarchLineChartSeries("web", label: "Web", values: web),
            MonarchLineChartSeries("ios", label: "iOS", values: ios),
            MonarchLineChartSeries("android", label: "Android", values: android),
        ]
    }

    static let fixtures = [
        Fixture("overlapping, with a target and inspection") {
            MonarchAreaChart(
                "Traffic",
                dates: days,
                series: platforms,
                description: "Daily sessions by platform",
                referenceLines: [MonarchLineChartReferenceLine(500, label: "Target")],
                activeIndex: .constant(4),
                dateFormat: utc
            )
        },
        Fixture("stacked with a gap, monotone, inspected total") {
            MonarchAreaChart(
                "Traffic",
                dates: days,
                series: platforms,
                stacking: .stacked,
                curve: .monotone,
                activeIndex: .constant(8),
                dateFormat: utc
            )
        },
        Fixture("100% stacked, inspected shares, a hidden series") {
            MonarchAreaChart(
                "Share of traffic",
                categories: ["Jan", "Feb", "Mar", "Apr"],
                series: [
                    MonarchLineChartSeries(
                        "a",
                        label: "A very long series name for the first platform",
                        values: [1, 2, 3, 4]
                    ),
                    MonarchLineChartSeries("b", label: "B", values: [3, 2, 1, 0]),
                    MonarchLineChartSeries("c", label: "C", values: [1, 1, -1, 1], isDashed: true),
                ],
                stacking: .expand,
                referenceLines: [MonarchLineChartReferenceLine(0.5)],
                hiddenSeries: .constant(["b"]),
                activeIndex: .constant(2)
            )
        },
        Fixture("one series, zero left out, fixed domain, custom format") {
            VStack {
                MonarchAreaChart(
                    "Users",
                    categories: ["Q1", "Q2", "Q3", "Q4"],
                    series: [
                        MonarchLineChartSeries("users", label: "Users", values: [1000, 1085, nil, 1255], color: .data3)
                    ],
                    includesZero: false,
                    height: 140,
                    formatValue: { "\(Int($0)) people" }
                )
                MonarchAreaChart(
                    "Users",
                    categories: ["Q1", "Q2"],
                    series: [MonarchLineChartSeries("users", label: "Users", values: [10, 20])],
                    valueDomain: 0...100,
                    isLegendInteractive: false,
                    height: 120
                )
            }
        },
        Fixture("loading, error and empty") {
            VStack {
                MonarchAreaChart("Traffic", dates: days, series: [], status: .loading, height: 80)
                MonarchAreaChart("Traffic", dates: days, series: [], status: .error, height: 80, onRetry: {})
                MonarchAreaChart(
                    "Traffic",
                    dates: [],
                    series: [],
                    emptyMessage: "Traffic appears after the first visit."
                )
            }
        },
        Fixture("popover with a total") {
            MonarchChartTooltip(
                title: "Mar 4",
                rows: [
                    MonarchChartTooltip.Row(key: "a", label: "Web", value: "480", role: \.data1, isDashed: false),
                    MonarchChartTooltip.Row(
                        key: "b",
                        label: "iOS",
                        value: "No value",
                        role: \.data2,
                        isDashed: true
                    ),
                ],
                footer: "Total 480",
                marker: .square
            )
        },
    ]
}

@Suite("MonarchAreaChart")
struct MonarchAreaChartTests {
    private let english = Locale(identifier: "en_US")

    private func resolved(_ values: [[Double?]], count: Int) -> [MonarchLineChartTokens.Series] {
        MonarchLineChartTokens.resolve(
            values.enumerated().map {
                MonarchLineChartSeries("s\($0.offset)", label: "S\($0.offset)", values: $0.element)
            },
            count: count
        )
    }

    @Test("Overlapping areas run from the baseline to each value; gaps stay gaps")
    func overlap() {
        let stack = MonarchAreaChartTokens.stack(
            resolved([[1, nil, 3], [2, 2, 2]], count: 3),
            count: 3,
            stacking: .overlap,
            baseline: 0
        )
        #expect(stack.layers[0].bands == [.init(low: 0, high: 1), nil, .init(low: 0, high: 3)])
        #expect(stack.layers[1].bands.map { $0?.high } == [2, 2, 2])
        #expect(stack.totals == [3, 2, 5])
        let raised = MonarchAreaChartTokens.stack(
            resolved([[200]], count: 1),
            count: 1,
            stacking: .overlap,
            baseline: 150
        )
        #expect(raised.layers[0].bands == [.init(low: 150, high: 200)])
    }

    @Test("Stacked areas pile up in order; negatives count as zero and a gap adds nothing")
    func stacked() {
        let stack = MonarchAreaChartTokens.stack(
            resolved([[1, 2, nil], [3, -4, 5], [1, 1, 1]], count: 3),
            count: 3,
            stacking: .stacked,
            baseline: 99
        )
        #expect(stack.layers[0].bands == [.init(low: 0, high: 1), .init(low: 0, high: 2), nil])
        #expect(stack.layers[1].bands == [.init(low: 1, high: 4), .init(low: 2, high: 2), .init(low: 0, high: 5)])
        #expect(stack.layers[2].bands == [.init(low: 4, high: 5), .init(low: 2, high: 3), .init(low: 5, high: 6)])
        #expect(stack.totals == [5, 3, 6])
    }

    @Test("A 100% stack piles shares of each x value's total and reaches one")
    func expand() {
        let stack = MonarchAreaChartTokens.stack(
            resolved([[1, 0], [3, 0]], count: 2),
            count: 2,
            stacking: .expand,
            baseline: 0
        )
        #expect(stack.layers[0].bands == [.init(low: 0, high: 0.25), .init(low: 0, high: 0)])
        #expect(stack.layers[1].bands == [.init(low: 0.25, high: 1), .init(low: 0, high: 0)])
        #expect(MonarchAreaChartTokens.stack([], count: 0, stacking: .expand, baseline: 0).layers.isEmpty)
    }

    @Test("Stacks start at zero, use 0 to 1 when expanded and drop reference lines there")
    func axes() {
        let series = resolved([[200, 300]], count: 2)
        #expect(MonarchAreaChartTokens.baseline(series, stacking: .overlap, domain: nil, includesZero: false) == 200)
        #expect(MonarchAreaChartTokens.baseline(series, stacking: .stacked, domain: nil, includesZero: false) == 0)
        #expect(MonarchAreaChartTokens.valueDomain(10...20, stacking: .expand) == 0...1)
        #expect(MonarchAreaChartTokens.valueDomain(10...20, stacking: .stacked) == 10...20)
        #expect(MonarchAreaChartTokens.valueDomain(nil, stacking: .overlap) == nil)
        #expect(MonarchAreaChartTokens.includesZero(false, stacking: .stacked))
        #expect(!MonarchAreaChartTokens.includesZero(false, stacking: .overlap))
        let lines = [MonarchLineChartReferenceLine(5)]
        #expect(MonarchAreaChartTokens.referenceLines(lines, stacking: .expand).isEmpty)
        #expect(MonarchAreaChartTokens.referenceLines(lines, stacking: .stacked) == lines)
    }

    @Test("Fills are faint when overlapping, stronger when stacked, fainter still when dashed")
    func fills() {
        let overlap = MonarchAreaChartTokens.fillOpacity(stacking: .overlap, isDashed: false)
        let stacked = MonarchAreaChartTokens.fillOpacity(stacking: .stacked, isDashed: false)
        let dashed = MonarchAreaChartTokens.fillOpacity(stacking: .stacked, isDashed: true)
        #expect(dashed < overlap)
        #expect(overlap < stacked)
        #expect(MonarchAreaChartTokens.fillOpacity(stacking: .expand, isDashed: false) == stacked)
    }

    @Test("Values read with their share in a 100% stack, and gaps read as No value")
    func values() {
        let share: (String, String) -> String = { MonarchAreaChartStrings.share($0, $1) }
        let text = { (value: Double?, stacking: MonarchAreaChartStacking) in
            MonarchAreaChartTokens.valueText(
                value,
                total: 400,
                stacking: stacking,
                noValue: "No value",
                locale: english,
                formatValue: nil,
                share: share
            )
        }
        #expect(text(120, .stacked) == "120")
        #expect(text(100, .expand) == "100 (25%)")
        #expect(text(-10, .expand) == "-10 (0%)")
        #expect(text(nil, .expand) == "No value")
        #expect(MonarchAreaChartTokens.shareText(1.0 / 3, locale: english) == "33.3%")
        #expect(MonarchAreaChartTokens.tickText(0.75, locale: english) == "75%")
    }

    @Test("The popover lists the stack top first and adds the total")
    func tooltip() {
        let stack = MonarchAreaChartTokens.stack(
            resolved([[100], [300]], count: 1),
            count: 1,
            stacking: .stacked,
            baseline: 0
        )
        let rows = MonarchAreaChartTokens.tooltipRows(
            stack,
            at: 0,
            stacking: .stacked,
            noValue: "-",
            locale: english,
            formatValue: nil,
            share: { "\($0)|\($1)" }
        )
        #expect(rows.map(\.label) == ["S1", "S0"])
        #expect(rows.map(\.value) == ["300", "100"])
        #expect(rows.map(\.role) == [\MonarchColorTokens.data2, \MonarchColorTokens.data1])
        #expect(
            MonarchAreaChartTokens.totalText(
                stack,
                at: 0,
                stacking: .stacked,
                totalLabel: "Total",
                locale: english,
                formatValue: nil
            )
                == "Total 400"
        )
        #expect(
            MonarchAreaChartTokens.totalText(
                stack,
                at: 0,
                stacking: .overlap,
                totalLabel: "Total",
                locale: english,
                formatValue: nil
            )
                == nil
        )
        #expect(
            MonarchAreaChartTokens.totalText(
                stack,
                at: 5,
                stacking: .stacked,
                totalLabel: "Total",
                locale: english,
                formatValue: nil
            )
                == nil
        )
        let overlap = MonarchAreaChartTokens.stack(
            resolved([[100], [300]], count: 1),
            count: 1,
            stacking: .overlap,
            baseline: 0
        )
        #expect(
            MonarchAreaChartTokens.tooltipRows(
                overlap,
                at: 0,
                stacking: .overlap,
                noValue: "-",
                locale: english,
                formatValue: nil,
                share: { "\($0)|\($1)" }
            ).map(\.label) == ["S0", "S1"]
        )
    }

    @Test("The data table keeps every series and adds a total column to a stack")
    func table() {
        let series = resolved([[100, nil], [300, 50]], count: 2)
        let stacked = MonarchAreaChartTokens.table(
            series,
            xLabels: ["Mar 1", "Mar 2"],
            xTitle: "Day",
            valueTitle: "Sessions",
            stacking: .stacked,
            totalLabel: "Total",
            locale: english,
            formatValue: nil
        )
        #expect(stacked.series.map(\.name) == ["S0", "S1", "Total"])
        #expect(stacked.series.last?.values == [400, 50])
        #expect(stacked.valueTitle == "Sessions")
        #expect(
            MonarchChartFrameTokens.rowAccessibilityLabel(stacked, row: 1, noValue: "No value")
                == "Mar 2, S0 No value, S1 50, Total 50"
        )
        let overlap = MonarchAreaChartTokens.table(
            series,
            xLabels: ["Mar 1", "Mar 2"],
            xTitle: "Day",
            valueTitle: nil,
            stacking: .overlap,
            totalLabel: "Total",
            locale: english,
            formatValue: nil
        )
        #expect(overlap.series.map(\.name) == ["S0", "S1"])
        let single = MonarchAreaChartTokens.table(
            resolved([[1]], count: 1),
            xLabels: ["Q1"],
            xTitle: "Quarter",
            valueTitle: nil,
            stacking: .expand,
            totalLabel: "Total",
            locale: english,
            formatValue: nil
        )
        #expect(single.series.count == 1)
    }

    @Test("A gap breaks a band into runs")
    func runs() {
        let stack = MonarchAreaChartTokens.stack(
            resolved([[1, 2, nil, 4]], count: 4),
            count: 4,
            stacking: .stacked,
            baseline: 0
        )
        #expect(MonarchAreaChartTokens.runs(stack.layers[0]) == [[0, 1], [3]])
    }

    @Test("The legend has an entry per series, or none for one series")
    func legend() {
        #expect(MonarchAreaChartTokens.legend(resolved([[1]], count: 1)).isEmpty)
        #expect(MonarchAreaChartTokens.legend(resolved([[1], [2]], count: 1)).map(\.key) == ["s0", "s1"])
    }

    @Test("The built-in strings come from the component's strings table")
    func strings() {
        #expect(MonarchAreaChartStrings.total == "Total")
        #expect(MonarchAreaChartStrings.share("120", "40%") == "120 (40%)")
    }
}
