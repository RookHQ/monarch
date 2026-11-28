import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

private let series = [
    MonarchChartLegendItem("revenue", label: "Revenue", value: "$48,200"),
    MonarchChartLegendItem("costs", label: "Costs", value: "$31,050"),
    MonarchChartLegendItem("refunds", label: "Refunds and chargebacks from the previous quarter"),
    MonarchChartLegendItem("target", label: "Target", color: .neutral, isDashed: true),
]

private let many = (1...7).map { MonarchChartLegendItem("s\($0)", label: "Series \($0)") }

enum MonarchChartLegendFixtures: ComponentFixtures {
    static let id = "chart-legend"
    static let fixtures = [
        Fixture("markers, plain") {
            VStack(alignment: .leading) {
                ForEach(MonarchChartLegendMarker.allCases, id: \.self) { marker in
                    MonarchChartLegend(series, marker: marker)
                }
            }
        },
        Fixture("every colour slot and neutral, vertical") {
            MonarchChartLegend(many, marker: .square, axis: .vertical)
        },
        Fixture("toggles with hidden series and Show all") {
            MonarchChartLegend(series, hiddenSeries: .constant(["costs", "target"]), marker: .dot)
        },
        Fixture("toggles, one left visible, disabled") {
            VStack(alignment: .leading) {
                MonarchChartLegend(
                    Array(series.prefix(2)),
                    hiddenSeries: .constant(["costs"]),
                    showsShowAll: false
                )
                MonarchChartLegend(series, hiddenSeries: .constant([])).disabled(true)
            }
        },
    ]
}

@Suite("MonarchChartLegend")
struct MonarchChartLegendTests {
    private let keys = ["a", "b", "c"]

    @Test("Slots are given in order and never cycled; a sixth series is neutral")
    func automaticColours() {
        let colours = (0..<7).map { MonarchChartLegendTokens.color(nil, index: $0) }
        #expect(colours == [.data1, .data2, .data3, .data4, .data5, .neutral, .neutral])
        #expect(MonarchChartLegendTokens.color(.data4, index: 0) == .data4)
    }

    @Test("Slots map to the data roles, and neutral to the subtle text role")
    func roles() {
        #expect(MonarchChartLegendTokens.role(.data1) == \MonarchColorTokens.data1)
        #expect(MonarchChartLegendTokens.role(.data5) == \MonarchColorTokens.data5)
        #expect(MonarchChartLegendTokens.role(.neutral) == \MonarchColorTokens.textSubtle)
        let roles = Set(MonarchChartColor.allCases.map(MonarchChartLegendTokens.role))
        #expect(roles.count == MonarchChartColor.allCases.count)
    }

    @Test("A hidden series has a disabled marker and a muted label")
    func hiddenAppearance() {
        #expect(MonarchChartLegendTokens.markerRole(.data2, isHidden: false) == \MonarchColorTokens.data2)
        #expect(MonarchChartLegendTokens.markerRole(.data2, isHidden: true) == \MonarchColorTokens.textDisabled)
        #expect(MonarchChartLegendTokens.labelRole(isHidden: false) == \MonarchColorTokens.textDefault)
        #expect(MonarchChartLegendTokens.labelRole(isHidden: true) == \MonarchColorTokens.textMuted)
    }

    @Test("Toggling hides and shows a series")
    func toggling() {
        let hidden = MonarchChartLegendTokens.toggled([], key: "b", isVisible: false, keys: keys, keepsOneVisible: true)
        #expect(hidden == ["b"])
        let shown = MonarchChartLegendTokens.toggled(
            ["b"],
            key: "b",
            isVisible: true,
            keys: keys,
            keepsOneVisible: true
        )
        #expect(shown.isEmpty)
    }

    @Test("The last visible series cannot be hidden unless allowed")
    func keepsOneVisible() {
        #expect(MonarchChartLegendTokens.isLocked("a", hidden: ["b", "c"], keys: keys, keepsOneVisible: true))
        #expect(!MonarchChartLegendTokens.isLocked("a", hidden: ["b"], keys: keys, keepsOneVisible: true))
        #expect(!MonarchChartLegendTokens.isLocked("a", hidden: ["b", "c"], keys: keys, keepsOneVisible: false))
        #expect(!MonarchChartLegendTokens.isLocked("b", hidden: ["b", "c"], keys: keys, keepsOneVisible: true))
        let kept = MonarchChartLegendTokens.toggled(
            ["b", "c"],
            key: "a",
            isVisible: false,
            keys: keys,
            keepsOneVisible: true
        )
        #expect(kept == ["b", "c"])
        let all = MonarchChartLegendTokens.toggled(
            ["b", "c"],
            key: "a",
            isVisible: false,
            keys: keys,
            keepsOneVisible: false
        )
        #expect(all == ["a", "b", "c"])
    }

    @Test("Show all shows the legend's own series and keeps unknown keys hidden")
    func showAll() {
        #expect(MonarchChartLegendTokens.showingAll(["a", "other"], keys: keys) == ["other"])
        #expect(MonarchChartLegendTokens.showsShowAll(hidden: ["a"], keys: keys, isEnabled: true))
        #expect(!MonarchChartLegendTokens.showsShowAll(hidden: ["other"], keys: keys, isEnabled: true))
        #expect(!MonarchChartLegendTokens.showsShowAll(hidden: ["a"], keys: keys, isEnabled: false))
    }

    @Test("VoiceOver reads the label, then the value")
    func accessibilityLabel() {
        #expect(MonarchChartLegendTokens.accessibilityLabel(label: "Revenue", value: "$48k") == "Revenue, $48k")
        #expect(MonarchChartLegendTokens.accessibilityLabel(label: "Revenue", value: nil) == "Revenue")
    }

    @Test("The built-in strings resolve")
    func strings() {
        #expect(MonarchChartLegendStrings.legend == "Legend")
        #expect(MonarchChartLegendStrings.showAll == "Show all")
    }

    @Test("Toggle entries keep a 44-point touch target")
    @MainActor
    func hitTarget() {
        let size = Host.layout(MonarchChartLegend(Array(series.prefix(1)), hiddenSeries: .constant([])))
        #expect(size.height >= MonarchAccessibility.minimumHitTarget)
    }
}
