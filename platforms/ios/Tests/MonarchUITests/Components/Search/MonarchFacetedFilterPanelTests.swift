import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

private let statusOptions = [
    MonarchFacetOption(id: "open", label: "Open", count: 128),
    MonarchFacetOption(id: "review", label: "In review", count: 42),
    MonarchFacetOption(id: "done", label: "Done", count: 1_204),
    MonarchFacetOption(id: "archived", label: "Archived", count: 0, isDisabled: true),
]

private let facetsFixture: [MonarchFacet] = [
    .options(id: "status", label: "Status", options: statusOptions, maxVisible: 2),
    .range(id: "size", label: "File size", bounds: 0...100, unit: "MB"),
    .options(id: "type", label: "Type", options: [], isInitiallyExpanded: false),
]

enum MonarchFacetedFilterPanelFixtures: ComponentFixtures {
    static let id = "faceted-filter-panel"
    static let fixtures = [
        Fixture("no filters") {
            MonarchFacetedFilterPanel(facets: facetsFixture, selection: .constant([:]))
        },
        Fixture("options and range active, checked past the limit") {
            MonarchFacetedFilterPanel(
                facets: facetsFixture,
                selection: .constant(["status": .options(["open", "done"]), "size": .range(10...25)])
            )
        },
        Fixture("collapsed, no title, long labels") {
            MonarchFacetedFilterPanel(
                facets: [
                    .options(
                        id: "owner",
                        label: "Owner of the record in the northern sales region",
                        options: [
                            MonarchFacetOption(
                                id: "a",
                                label: "Alexandra Kim-Rivera from the platform engineering team",
                                count: 12_345
                            )
                        ],
                        isInitiallyExpanded: false
                    )
                ],
                selection: .constant(["owner": .options(["a"])]),
                showsTitle: false
            )
        },
        Fixture("disabled, empty") {
            VStack(alignment: .leading) {
                MonarchFacetedFilterPanel(facets: facetsFixture, selection: .constant([:])).disabled(true)
                MonarchFacetedFilterPanel(facets: [], selection: .constant([:]))
            }
        },
    ]
}

@Suite("MonarchFacetedFilterPanel")
struct MonarchFacetedFilterPanelTests {
    private let english = Locale(identifier: "en_US")
    private let text = MonarchFacetedFilterPanelText(labels: MonarchFacetedFilterPanelLabels())

    @Test("Active counts: checked options, and one for a range")
    func activeCounts() {
        let selection: [String: MonarchFacetSelection] = [
            "status": .options(["open", "done"]), "size": .range(0...25),
        ]
        #expect(MonarchFacetedFilterPanelTokens.activeCount(facetsFixture[0], in: selection) == 2)
        #expect(MonarchFacetedFilterPanelTokens.activeCount(facetsFixture[1], in: selection) == 1)
        #expect(MonarchFacetedFilterPanelTokens.activeCount(facetsFixture[2], in: selection) == 0)
        #expect(MonarchFacetedFilterPanelTokens.totalActive(facetsFixture, in: selection) == 3)
        #expect(MonarchFacetedFilterPanelTokens.totalActive(facetsFixture, in: [:]) == 0)
    }

    @Test("A value of the wrong kind for its facet is not counted")
    func mismatched() {
        let selection: [String: MonarchFacetSelection] = ["status": .range(0...1), "size": .options(["x"])]
        #expect(MonarchFacetedFilterPanelTokens.totalActive(facetsFixture, in: selection) == 0)
    }

    @Test("Checking adds the option; clearing the last one removes the facet")
    func setting() {
        var selection = MonarchFacetedFilterPanelTokens.setting("open", isOn: true, facetID: "status", in: [:])
        #expect(selection == ["status": .options(["open"])])
        selection = MonarchFacetedFilterPanelTokens.setting("done", isOn: true, facetID: "status", in: selection)
        #expect(selection == ["status": .options(["open", "done"])])
        selection = MonarchFacetedFilterPanelTokens.setting("open", isOn: false, facetID: "status", in: selection)
        selection = MonarchFacetedFilterPanelTokens.setting("done", isOn: false, facetID: "status", in: selection)
        #expect(selection.isEmpty)
    }

    @Test("A range equal to the bounds is no filter")
    func ranges() {
        let bounds = 0.0...100.0
        #expect(MonarchFacetedFilterPanelTokens.range("size", bounds: bounds, in: [:]) == bounds)
        let set = MonarchFacetedFilterPanelTokens.setting(10...25, bounds: bounds, facetID: "size", in: [:])
        #expect(set == ["size": .range(10...25)])
        #expect(MonarchFacetedFilterPanelTokens.range("size", bounds: bounds, in: set) == 10...25)
        #expect(MonarchFacetedFilterPanelTokens.setting(bounds, bounds: bounds, facetID: "size", in: set).isEmpty)
        #expect(
            MonarchFacetedFilterPanelTokens.range("size", bounds: bounds, in: ["size": .range(-5...500)]) == bounds
        )
    }

    @Test("Clearing one facet keeps the others")
    func clearing() {
        let selection: [String: MonarchFacetSelection] = ["status": .options(["open"]), "size": .range(0...25)]
        #expect(MonarchFacetedFilterPanelTokens.clearing("status", in: selection) == ["size": .range(0...25)])
    }

    @Test("Options past the limit hide, except checked ones")
    func visibleOptions() {
        let shown = MonarchFacetedFilterPanelTokens.visibleOptions(
            statusOptions,
            checked: ["archived"],
            maxVisible: 2,
            isShowingAll: false
        )
        #expect(shown.options.map(\.id) == ["open", "review", "archived"])
        #expect(shown.hidden == 1)
        let all = MonarchFacetedFilterPanelTokens.visibleOptions(
            statusOptions,
            checked: [],
            maxVisible: 2,
            isShowingAll: true
        )
        #expect(all.options.count == 4)
        #expect(all.hidden == 0)
        #expect(
            MonarchFacetedFilterPanelTokens.visibleOptions(
                statusOptions,
                checked: [],
                maxVisible: 0,
                isShowingAll: false
            )
            .hidden == 4
        )
    }

    @Test("Counts are part of the spoken name and use the locale")
    func accessibility() {
        #expect(
            MonarchFacetedFilterPanelTokens.optionAccessibilityLabel(statusOptions[2], locale: english) == "Done, 1,204"
        )
        #expect(
            MonarchFacetedFilterPanelTokens.optionAccessibilityLabel(
                statusOptions[2],
                locale: Locale(identifier: "de_DE")
            )
                == "Done, 1.204"
        )
        #expect(
            MonarchFacetedFilterPanelTokens.optionAccessibilityLabel(
                MonarchFacetOption(id: "x", label: "No count"),
                locale: english
            ) == "No count"
        )
        #expect(MonarchFacetedFilterPanelTokens.rangeValueText(25, unit: "MB", locale: english) == "25 MB")
        #expect(MonarchFacetedFilterPanelTokens.rangeValueText(2_500, unit: nil, locale: english) == "2,500")
    }

    @Test("Colours: muted counts, accent active count, disabled roles")
    func colors() {
        #expect(MonarchFacetedFilterPanelTokens.countColor(isEnabled: true) == \.textMuted)
        #expect(MonarchFacetedFilterPanelTokens.countColor(isEnabled: false) == \.textDisabled)
        #expect(MonarchFacetedFilterPanelTokens.activeCountColor(isEnabled: true) == \.accentStrong)
        #expect(MonarchFacetedFilterPanelTokens.activeCountColor(isEnabled: false) == \.textDisabled)
    }

    @Test("Built-in strings come from the table")
    func builtIn() {
        #expect(text.title == "Filters")
        #expect(text.clearAll == "Clear all")
        #expect(text.clearGroup == "Clear")
        #expect(text.clearGroupLabel("Status") == "Clear Status filter")
        #expect(text.activeCount(2) == "2 selected")
        #expect(text.showMore(4) == "Show 4 more")
        #expect(text.showFewer == "Show fewer")
        #expect(text.noOptions == "No options")
    }

    @Test("Every string can be replaced")
    func replaced() {
        let custom = MonarchFacetedFilterPanelText(
            labels: MonarchFacetedFilterPanelLabels(
                title: "Refine",
                clearGroupLabel: { "Reset \($0)" },
                activeCount: { "\($0) on" },
                showMore: { "+\($0)" }
            )
        )
        #expect(custom.title == "Refine")
        #expect(custom.clearGroupLabel("Status") == "Reset Status")
        #expect(custom.activeCount(3) == "3 on")
        #expect(custom.showMore(2) == "+2")
    }

    #if os(iOS)
        @Test("The clear-all button keeps a 44-point touch target")
        @MainActor
        func hitTarget() {
            let panel = MonarchFacetedFilterPanel(facets: [], selection: .constant([:])).fixedSize()
            #expect(Host.layout(panel).height >= MonarchAccessibility.minimumHitTarget)
        }
    #endif
}
