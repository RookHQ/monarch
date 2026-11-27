import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchBarListFixtures: ComponentFixtures {
    static let id = "bar-list"
    static let fixtures = [
        Fixture("ranked with a limit, an icon, a description and a missing value") {
            MonarchBarList(
                "Top pages",
                items: [
                    MonarchBarListItem("/pricing", value: 4_210, description: "Campaign: spring", systemImage: "tag"),
                    MonarchBarListItem("/docs", value: 2_980, isHighlighted: true),
                    MonarchBarListItem("/blog", value: nil),
                    MonarchBarListItem("/about", value: 310, displayValue: "310 visits"),
                ],
                limit: 3,
                isRanked: true
            )
        },
        Fixture("tappable rows, ascending, fixed maximum and custom format") {
            MonarchBarList(
                "Conversion",
                items: [
                    MonarchBarListItem("Email", value: 0.12),
                    MonarchBarListItem("Search", value: 0.31),
                    MonarchBarListItem(
                        "A very long referrer name that has to wrap onto a second line of text",
                        value: 0.08
                    ),
                ],
                sort: .ascending,
                maximum: 1,
                formatValue: { $0.formatted(.percent) },
                onSelect: { _ in }
            )
        },
        Fixture("empty and loading") {
            VStack(alignment: .leading) {
                MonarchBarList("Top pages", items: [])
                MonarchBarList("Top pages", items: [], emptyLabel: "Nothing was visited")
                MonarchBarList("Top pages", items: [], isLoading: true)
            }
        },
        Fixture("failed, with and without retry") {
            VStack(alignment: .leading) {
                MonarchBarList("Top pages", items: [], error: "The pages could not be loaded.", onRetry: {})
                MonarchBarList("Top pages", items: [], error: "The pages could not be loaded.")
                MonarchBarList("Top pages", items: [], isRanked: true, isLoading: true, loadingRows: 3)
            }
        },
        Fixture("all zero and given order") {
            MonarchBarList(
                items: [MonarchBarListItem("A", value: 0), MonarchBarListItem("B", value: 0)],
                sort: .none,
                overflowLabel: "More"
            )
        },
    ]
}

@Suite("MonarchBarList")
struct MonarchBarListTests {
    private let english = Locale(identifier: "en_US")
    private let items = [
        MonarchBarListItem("b", value: 20),
        MonarchBarListItem("none", value: nil),
        MonarchBarListItem("a", value: 40),
        MonarchBarListItem("c", value: 10),
        MonarchBarListItem("tie", value: 20),
    ]

    private func labels(_ sort: MonarchBarListSort, limit: Int? = nil) -> [String] {
        MonarchBarListTokens.layout(items: items, sort: sort, maximum: nil, limit: limit).rows.map(\.item.label)
    }

    @Test("Rows are sorted by value, ties keep their order and missing values go last")
    func sorting() {
        #expect(labels(.descending) == ["a", "b", "tie", "c", "none"])
        #expect(labels(.ascending) == ["c", "b", "tie", "a", "none"])
        #expect(labels(.none) == ["b", "none", "a", "c", "tie"])
    }

    @Test("The bar is the share of the largest value, and a missing value has no bar")
    func fractions() {
        let layout = MonarchBarListTokens.layout(items: items, sort: .descending, maximum: nil, limit: nil)
        #expect(layout.rows.map(\.fraction) == [1, 0.5, 0.5, 0.25, 0])
        #expect(layout.rows.map(\.rank) == [1, 2, 3, 4, 5])
    }

    @Test("A fixed maximum sets the scale and longer bars stop at the end")
    func maximum() {
        let layout = MonarchBarListTokens.layout(items: items, sort: .descending, maximum: 80, limit: nil)
        #expect(layout.rows.map(\.fraction) == [0.5, 0.25, 0.25, 0.125, 0])
        let small = MonarchBarListTokens.layout(items: items, sort: .descending, maximum: 20, limit: nil)
        #expect(small.rows.first?.fraction == 1)
    }

    @Test("A limit hides rows but keeps the total and the scale of all items")
    func limit() {
        let layout = MonarchBarListTokens.layout(items: items, sort: .ascending, maximum: nil, limit: 2)
        #expect(layout.rows.map(\.item.label) == ["c", "b"])
        #expect(layout.total == 5)
        // 10 of 40: the hidden largest value still sets the scale.
        #expect(layout.rows.first?.fraction == 0.25)
        #expect(MonarchBarListTokens.layout(items: items, sort: .none, maximum: nil, limit: -1).rows.isEmpty)
    }

    @Test("Zero, negative and non-finite values never draw outside the row")
    func extremes() {
        let odd = [
            MonarchBarListItem("zero", value: 0),
            MonarchBarListItem("negative", value: -5),
            MonarchBarListItem("infinite", value: .infinity),
            MonarchBarListItem("nan", value: .nan),
        ]
        let layout = MonarchBarListTokens.layout(items: odd, sort: .descending, maximum: nil, limit: nil)
        #expect(layout.rows.allSatisfy { $0.fraction == 0 })
        // Values that are not finite are treated as missing: last and without a value.
        #expect(layout.rows.map(\.item.label) == ["zero", "negative", "infinite", "nan"])
        #expect(MonarchBarListTokens.valueText(for: odd[2], locale: english, formatValue: nil) == nil)
    }

    @Test("The value is the display value, else the custom format, else a number in the locale")
    func valueText() {
        let plain = MonarchBarListItem("a", value: 4_210)
        #expect(MonarchBarListTokens.valueText(for: plain, locale: english, formatValue: nil) == "4,210")
        #expect(
            MonarchBarListTokens.valueText(for: plain, locale: Locale(identifier: "de_DE"), formatValue: nil) == "4.210"
        )
        #expect(
            MonarchBarListTokens.valueText(for: plain, locale: english, formatValue: { "\(Int($0)) visits" })
                == "4210 visits"
        )
        let custom = MonarchBarListItem("a", value: 4_210, displayValue: "12% · 4 of 33")
        #expect(
            MonarchBarListTokens.valueText(for: custom, locale: english, formatValue: { _ in "x" }) == "12% · 4 of 33"
        )
        #expect(
            MonarchBarListTokens.valueText(for: MonarchBarListItem("a", value: nil), locale: english, formatValue: nil)
                == nil
        )
    }

    @Test("VoiceOver reads the label, then the description")
    func accessibilityLabel() {
        #expect(MonarchBarListTokens.accessibilityLabel(for: MonarchBarListItem("/docs", value: 1)) == "/docs")
        #expect(
            MonarchBarListTokens.accessibilityLabel(for: MonarchBarListItem("/docs", value: 1, description: "Spring"))
                == "/docs, Spring"
        )
    }

    @Test("A highlighted row is heavier and has a stronger bar")
    func highlight() {
        #expect(MonarchBarListTokens.bar(isHighlighted: false) == \MonarchColorTokens.data1)
        #expect(MonarchBarListTokens.bar(isHighlighted: true) == \MonarchColorTokens.data1)
        #expect(MonarchBarListTokens.labelWeight(isHighlighted: true) == .semibold)
        #expect(MonarchBarListTokens.labelWeight(isHighlighted: false) == .medium)
    }

    @Test("The built-in strings come from the component's strings table")
    func strings() {
        #expect(MonarchBarListStrings.missing == "No value")
        #expect(MonarchBarListStrings.empty == "No data yet")
        #expect(MonarchBarListStrings.loading == "Loading")
        #expect(MonarchBarListStrings.overflow(shown: "5", total: "12") == "Top 5 of 12")
    }

    @Test("Placeholder rows fall in length, at least one and at most twelve")
    func placeholders() {
        let three = MonarchBarListTokens.placeholderItems(count: 3)
        #expect(three.map(\.value) == [3, 2, 1])
        #expect(Set(three.map(\.id)).count == 3)
        #expect(MonarchBarListTokens.placeholderItems(count: 0).count == 1)
        #expect(MonarchBarListTokens.placeholderItems(count: 99).count == 12)
    }
}
