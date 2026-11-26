import Accessibility
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

private let devices = [
    MonarchDonutChartSegment("phone", label: "Phones", value: 268),
    MonarchDonutChartSegment("laptop", label: "Laptops", value: 412),
    MonarchDonutChartSegment("monitor", label: "Monitors", value: 131),
    MonarchDonutChartSegment("tablet", label: "Tablets", value: 54),
    MonarchDonutChartSegment("other", label: "Accessories", value: 9),
]

private func whole(_ value: Double) -> String { "\(Int(value))" }

private func percent(_ ratio: Double, digits: Int = 0) -> String {
    ratio.formatted(.percent.precision(.fractionLength(0...digits)))
}

enum MonarchDonutChartFixtures: ComponentFixtures {
    static let id = "donut-chart"
    static let fixtures = [
        Fixture("default: total in the middle, legend with values and shares") {
            MonarchDonutChart(
                "Assets by type",
                segments: devices,
                description: "All active assets",
                centerLabel: "assets",
                footer: "Updated hourly."
            )
        },
        Fixture("every legend content") {
            VStack {
                ForEach(MonarchDonutChartLegendContent.allCases, id: \.self) { content in
                    MonarchDonutChart("Assets", segments: devices, legendContent: content, size: 96)
                }
            }
        },
        Fixture("folded into Other, given order, pinned colour, one decimal") {
            MonarchDonutChart(
                "Tickets by channel",
                segments: [
                    MonarchDonutChartSegment("email", label: "Email", value: 40, color: .data3),
                    MonarchDonutChartSegment("chat", label: "Chat", value: 100),
                    MonarchDonutChartSegment("phone", label: "Phone", value: 0.2),
                    MonarchDonutChartSegment("social", label: "Social", value: -5),
                    MonarchDonutChartSegment("forum", label: "Forum", value: 12),
                ],
                sort: .none,
                maxSegments: 3,
                percentDigits: 1
            )
        },
        Fixture("custom centre, no centre value, no legend, thick and thin rings") {
            VStack {
                MonarchDonutChart(
                    "Storage",
                    segments: devices,
                    centerValue: "68%",
                    centerLabel: "of 2 TB used in the shared workspace",
                    size: 200,
                    thickness: 60
                )
                MonarchDonutChart(
                    "Storage",
                    segments: devices,
                    size: 120,
                    thickness: 2,
                    showsCenterValue: false,
                    showsLegend: false
                )
            }
        },
        Fixture("one segment and long labels") {
            MonarchDonutChart(
                "Plan",
                segments: [
                    MonarchDonutChartSegment(
                        "enterprise",
                        label: "Enterprise annual contracts with volume discounts",
                        value: 12
                    )
                ]
            )
        },
        Fixture("loading, empty by zero total, error with retry, custom labels") {
            VStack {
                MonarchDonutChart("Assets", segments: devices, status: .loading)
                MonarchDonutChart(
                    "Assets",
                    segments: [MonarchDonutChartSegment("a", label: "A", value: 0)],
                    emptyMessage: "Assets appear after the first sync."
                )
                MonarchDonutChart("Assets", segments: devices, status: .error, onRetry: {})
                MonarchDonutChart(
                    "Assets",
                    segments: devices,
                    isTableToggleShown: false,
                    labels: MonarchDonutChartLabels(other: "Rest", total: "Summe")
                )
            }
        },
    ]
}

@Suite("MonarchDonutChart")
struct MonarchDonutChartTests {
    @Test("Negative and non-finite amounts count as 0")
    func clean() {
        #expect(MonarchDonutChartTokens.clean(5) == 5)
        #expect(MonarchDonutChartTokens.clean(-5) == 0)
        #expect(MonarchDonutChartTokens.clean(.nan) == 0)
        #expect(MonarchDonutChartTokens.clean(.infinity) == 0)
    }

    @Test("Largest first by default, ties keep their order, colours follow the sorted position")
    func ordering() {
        let ordered = MonarchDonutChartTokens.ordered(devices, sort: .descending)
        #expect(ordered.map(\.key) == ["laptop", "phone", "monitor", "tablet", "other"])
        #expect(ordered.map(\.color) == [.data1, .data2, .data3, .data4, .data5])
        let given = MonarchDonutChartTokens.ordered(devices, sort: .none)
        #expect(given.map(\.key) == devices.map(\.key))
        let ties = MonarchDonutChartTokens.ordered(
            [
                MonarchDonutChartSegment("a", label: "A", value: 1),
                MonarchDonutChartSegment("b", label: "B", value: 1, color: .data5),
            ],
            sort: .descending
        )
        #expect(ties.map(\.key) == ["a", "b"])
        #expect(ties[1].color == .data5)
    }

    @Test("Beyond the limit the smallest fold into a neutral Other; zero slices are not drawn")
    func folding() {
        let ordered = MonarchDonutChartTokens.ordered(devices, sort: .descending)
        let three = MonarchDonutChartTokens.slices(ordered, maxSegments: 3, other: "Other")
        #expect(three.map(\.label) == ["Laptops", "Phones", "Other"])
        #expect(three[2].value == 194)
        #expect(three[2].color == .neutral)
        #expect(three[2].key == MonarchDonutChartTokens.otherKey)
        #expect(MonarchDonutChartTokens.slices(ordered, maxSegments: 6, other: "Other").count == 5)
        #expect(MonarchDonutChartTokens.slices(ordered, maxSegments: 0, other: "Other").count == 2)
        let zero = MonarchDonutChartTokens.ordered(
            [MonarchDonutChartSegment("a", label: "A", value: 3), MonarchDonutChartSegment("b", label: "B", value: 0)],
            sort: .none
        )
        #expect(MonarchDonutChartTokens.slices(zero, maxSegments: 6, other: "Other").map(\.key) == ["a"])
    }

    @Test("A total of 0 shows the empty state; other statuses are kept")
    func status() {
        #expect(MonarchDonutChartTokens.resolvedStatus(.ready, total: 0) == .empty)
        #expect(MonarchDonutChartTokens.resolvedStatus(.ready, total: 1) == .ready)
        #expect(MonarchDonutChartTokens.resolvedStatus(.loading, total: 0) == .loading)
        #expect(MonarchDonutChartTokens.resolvedStatus(.error, total: 0) == .error)
    }

    @Test("Ring geometry: 22% by default, clamped, and the gap only between slices")
    func geometry() {
        #expect(MonarchDonutChartTokens.thickness(nil, size: 160) == 35)
        #expect(MonarchDonutChartTokens.thickness(200, size: 160) == 80)
        #expect(MonarchDonutChartTokens.thickness(1, size: 160) == 4)
        #expect(MonarchDonutChartTokens.innerRatio(thickness: 40, size: 160) == 0.5)
        #expect(MonarchDonutChartTokens.innerRatio(thickness: 80, size: 160) == 0)
        #expect(MonarchDonutChartTokens.innerRatio(thickness: 10, size: 0) == 0)
        #expect(MonarchDonutChartTokens.angularInset(sliceCount: 1) == 0)
        #expect(MonarchDonutChartTokens.angularInset(sliceCount: 3) == 1)
    }

    @Test("Shares never round a segment away")
    func shares() {
        #expect(MonarchDonutChartTokens.share(1, of: 4, digits: 0) == percent(0.25))
        #expect(MonarchDonutChartTokens.share(1, of: 1000, digits: 0) == "<" + percent(0.01))
        #expect(MonarchDonutChartTokens.share(999, of: 1000, digits: 0) == ">" + percent(0.99))
        #expect(MonarchDonutChartTokens.share(1, of: 1000, digits: 1) == percent(0.001, digits: 1))
        #expect(MonarchDonutChartTokens.share(0, of: 10, digits: 0) == percent(0))
        #expect(MonarchDonutChartTokens.share(5, of: 0, digits: 0) == percent(0))
        #expect(MonarchDonutChartTokens.share(10, of: 10, digits: 0) == percent(1))
    }

    @Test("Legend entries show the value, the share, both or neither")
    func legendValue() {
        #expect(MonarchDonutChartTokens.legendValue(value: "412", share: "48%", content: .both) == "412 · 48%")
        #expect(MonarchDonutChartTokens.legendValue(value: "412", share: "48%", content: .value) == "412")
        #expect(MonarchDonutChartTokens.legendValue(value: "412", share: "48%", content: .percent) == "48%")
        #expect(MonarchDonutChartTokens.legendValue(value: "412", share: "48%", content: .none) == nil)
    }

    @Test("The summary gives the total, then each segment above 0 with value and share")
    func summary() {
        let ordered = MonarchDonutChartTokens.ordered(
            [
                MonarchDonutChartSegment("a", label: "A", value: 3),
                MonarchDonutChartSegment("b", label: "B", value: 1),
                MonarchDonutChartSegment("c", label: "C", value: 0),
            ],
            sort: .descending
        )
        let text = MonarchDonutChartTokens.summary(ordered, totalLabel: "Total", format: whole, digits: 0)
        #expect(text == "Total 4. A 3 (\(percent(0.75))), B 1 (\(percent(0.25)))")
    }

    @Test("The table lists every segment, then the total, with values and shares")
    func table() {
        let ordered = MonarchDonutChartTokens.ordered(devices, sort: .descending)
        let table = MonarchDonutChartTokens.table(
            ordered,
            segmentTitle: "Segment",
            valueTitle: "Value",
            shareTitle: "Share",
            totalLabel: "Total",
            format: whole,
            digits: 0
        )
        #expect(table.categoryTitle == "Segment")
        #expect(table.categories == ["Laptops", "Phones", "Monitors", "Tablets", "Accessories", "Total"])
        #expect(table.series.map(\.name) == ["Value", "Share"])
        #expect(table.series[0].formattedValues?.last == "874")
        #expect(table.series[1].formattedValues?.last == percent(1))
        #expect(table.series[1].values.last == 100)
    }

    @Test("VoiceOver's chart descriptor plays one value per segment, without the total")
    @MainActor
    func descriptor() {
        let ordered = MonarchDonutChartTokens.ordered(devices, sort: .descending)
        let table = MonarchDonutChartTokens.descriptorTable(
            ordered,
            segmentTitle: "Segment",
            valueTitle: "Value",
            format: whole
        )
        let chart = MonarchChartFrameDescriptor(title: "Assets", summary: "Total 874", table: table)
            .makeChartDescriptor()
        #expect(chart.summary == "Total 874")
        #expect(chart.series.count == 1)
        #expect(chart.series[0].dataPoints.count == 5)
        #expect(chart.series[0].dataPoints.first?.label == "412")
        #expect((chart.xAxis as? AXCategoricalDataAxisDescriptor)?.categoryOrder.count == 5)
    }

    @Test("The built-in strings resolve")
    func strings() {
        #expect(MonarchDonutChartStrings.other == "Other")
        #expect(MonarchDonutChartStrings.segment == "Segment")
        #expect(MonarchDonutChartStrings.value == "Value")
        #expect(MonarchDonutChartStrings.share == "Share")
        #expect(MonarchDonutChartStrings.total == "Total")
    }

    #if os(iOS)
        @Test("The chart keeps at least the donut's size in every status")
        @MainActor
        func keepsHeight() {
            for status in MonarchChartFrameStatus.allCases {
                let size = Host.layout(MonarchDonutChart("Assets", segments: devices, status: status, size: 180))
                #expect(size.height >= 180)
            }
        }
    #endif
}
