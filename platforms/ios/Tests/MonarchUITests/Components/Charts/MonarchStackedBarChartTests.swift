import Foundation
import SwiftUI
import Testing

@testable import MonarchUI

private let months = ["Jan", "Feb", "Mar"]

private let statuses = [
    MonarchBarChartSeries("resolved", label: "Resolved", values: [60, 30, nil]),
    MonarchBarChartSeries("pending", label: "Pending", values: [30, 10, 0]),
    MonarchBarChartSeries("escalated", label: "Escalated", values: [10, -5, 0], color: .neutral),
]

private let english = Locale(identifier: "en_US")

private func percent(_ value: Double) -> String {
    MonarchStackedBarChartTokens.percentText(value, locale: english)
}

enum MonarchStackedBarChartFixtures: ComponentFixtures {
    static let id = "stacked-bar-chart"
    static let fixtures = [
        Fixture("stacked columns with totals, highlight and a target") {
            MonarchStackedBarChart(
                "Tickets by status",
                categories: months,
                series: statuses,
                description: "First quarter",
                highlightedIndex: 1,
                referenceLines: [MonarchBarChartReferenceLine(80, label: "Target")],
                showsValueLabels: true,
                footer: "Source: help desk.",
                height: 160
            )
        },
        Fixture("100% horizontal bars, a hidden series, long names") {
            MonarchStackedBarChart(
                "Work by status",
                categories: ["Platform", "Design systems and accessibility tooling", "Growth"],
                series: statuses,
                categoryTitle: "Team",
                orientation: .horizontal,
                offset: .expand,
                hiddenSeries: .constant(["pending"]),
                showsValueLabels: true
            )
        },
        Fixture("100% columns, grouped ignores the offset") {
            VStack {
                MonarchStackedBarChart("Mix", categories: months, series: statuses, offset: .expand, height: 120)
                MonarchStackedBarChart(
                    "Mix",
                    categories: months,
                    series: statuses,
                    layout: .grouped,
                    offset: .expand,
                    height: 120
                )
            }
        },
        Fixture("loading, empty, error, custom labels") {
            VStack {
                MonarchStackedBarChart("Mix", categories: months, series: statuses, status: .loading, height: 80)
                MonarchStackedBarChart("Mix", categories: [], series: statuses, emptyMessage: "Nothing yet.")
                MonarchStackedBarChart(
                    "Mix",
                    categories: months,
                    series: statuses,
                    status: .error,
                    height: 80,
                    onRetry: {}
                )
                MonarchStackedBarChart(
                    "Mischung",
                    categories: months,
                    series: statuses,
                    offset: .expand,
                    isLegendToggleable: false,
                    isTableToggleShown: false,
                    height: 80,
                    labels: MonarchStackedBarChartLabels(
                        bar: MonarchBarChartLabels(total: "Summe"),
                        shareSeries: "%1$@: Anteil %2$@, max. %3$@ %4$@"
                    ),
                    onSelect: { _ in }
                )
            }
        },
    ]
}

@Suite("MonarchStackedBarChart")
struct MonarchStackedBarChartTests {
    @Test("Only stacked bars with the 100% offset draw shares")
    func expanded() {
        #expect(MonarchStackedBarChartTokens.isExpanded(layout: .stacked, offset: .expand))
        #expect(!MonarchStackedBarChartTokens.isExpanded(layout: .stacked, offset: .normal))
        #expect(!MonarchStackedBarChartTokens.isExpanded(layout: .grouped, offset: .expand))
        #expect(!MonarchStackedBarChartTokens.isExpanded(layout: .grouped, offset: .normal))
        #expect(MonarchStackedBarChartTokens.expandedRange == 0...1)
    }

    @Test("Shares divide by the positive total of every series; gaps stay gaps; negatives count as zero")
    func shares() {
        let shares = MonarchStackedBarChartTokens.shares(categories: months, series: statuses)
        #expect(shares.map(\.key) == ["resolved", "pending", "escalated"])
        #expect(shares.map(\.label) == ["Resolved", "Pending", "Escalated"])
        #expect(shares[2].color == .neutral)
        // Jan: 60 + 30 + 10 = 100.
        #expect(shares[0].values[0] == 0.6)
        #expect(shares[1].values[0] == 0.3)
        #expect(shares[2].values[0] == 0.1)
        // Feb: the negative value is left out of the total and drawn as zero.
        #expect(shares[0].values[1] == 0.75)
        #expect(shares[1].values[1] == 0.25)
        #expect(shares[2].values[1] == 0)
        // Mar: a gap stays a gap; a total of zero gives zero shares.
        #expect(shares[0].values[2] == nil)
        #expect(shares[1].values[2] == 0)
        #expect(shares[2].values[2] == 0)
    }

    @Test("Every category's shares add up to one when it has a positive total")
    func sharesAddUp() {
        let shares = MonarchStackedBarChartTokens.shares(categories: months, series: statuses)
        for index in 0..<2 {
            let sum = shares.reduce(0.0) { $0 + ($1.values[index] ?? 0) }
            #expect(abs(sum - 1) < 1e-9)
        }
    }

    @Test("Values that are not finite are gaps, and missing values beyond the series are gaps")
    func notFinite() {
        let series = [MonarchBarChartSeries("a", label: "A", values: [.nan, 4])]
        let shares = MonarchStackedBarChartTokens.shares(categories: ["x", "y", "z"], series: series)
        #expect(shares[0].values == [nil, 1, nil])
    }

    @Test("Shares are written as percentages with at most one decimal")
    func percentages() {
        #expect(percent(0.25) == "25%")
        #expect(percent(1.0 / 3.0) == "33.3%")
        #expect(percent(1) == "100%")
        #expect(MonarchStackedBarChartTokens.percentText(0.5, locale: Locale(identifier: "fr_FR")).contains("50"))
    }

    @Test("The share summary reads each shown series' average and largest share")
    func summary() {
        let shares = MonarchStackedBarChartTokens.shares(categories: months, series: statuses)
        let text = MonarchStackedBarChartTokens.shareSummary(
            categories: months,
            shares: shares,
            hidden: ["escalated"],
            format: percent,
            seriesFormat: "%1$@: average share %2$@, largest %3$@ at %4$@",
            noData: "No data"
        )
        #expect(text.hasPrefix(MonarchBarChartStrings.points(count: "3", first: "Jan", last: "Mar")))
        #expect(text.contains("Resolved: average share 67.5%, largest Feb at 75%"))
        #expect(text.contains("Pending: average share 18.3%, largest Jan at 30%"))
        #expect(!text.contains("Escalated"))
    }

    @Test("The share summary of no categories, one category and a series without values")
    func summaryEdges() {
        #expect(
            MonarchStackedBarChartTokens.shareSummary(
                categories: [],
                shares: [],
                hidden: [],
                format: percent,
                seriesFormat: "%1$@ %2$@ %3$@ %4$@",
                noData: "No data"
            ) == "No data"
        )
        let empty = [MonarchBarChartSeries("a", label: "A", values: [nil])]
        let text = MonarchStackedBarChartTokens.shareSummary(
            categories: ["Q1"],
            shares: empty,
            hidden: [],
            format: percent,
            seriesFormat: "%1$@ %2$@ %3$@ %4$@",
            noData: "No data"
        )
        #expect(text == MonarchBarChartStrings.onePoint("Q1") + ". A: No data")
    }

    @Test("The built-in string resolves and the labels override it")
    func strings() {
        #expect(MonarchStackedBarChartStrings.shareSeries != "monarch.stackedBarChart.shareSeries")
        #expect(MonarchStackedBarChartStrings.shareSeries.contains("%4$@"))
        let labels = MonarchStackedBarChartLabels(shareSeries: "%1$@ %2$@ %3$@ %4$@")
        #expect(MonarchStackedBarChartStrings.fill(labels.shareSeries ?? "", "A", "1", "B", "2") == "A 1 B 2")
    }
}
