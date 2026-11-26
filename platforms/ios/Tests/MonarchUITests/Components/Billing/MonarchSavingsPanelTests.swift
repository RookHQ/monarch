import Foundation
import SwiftUI
import Testing

@_spi(Experimental) @testable import MonarchUI

private let calendar: Calendar = {
    var calendar = Calendar(identifier: .gregorian)
    calendar.timeZone = .gmt
    return calendar
}()

/// 2026-11-14 09:00 UTC.
private let now = Date(timeIntervalSince1970: 1_794_646_800)

private let statuses = MonarchStatusRegistry([
    "parked": MonarchStatusDefinition("Parked", tone: .warning, systemImage: "parkingsign.circle"),
    "no_site": MonarchStatusDefinition("No website", tone: .neutral, systemImage: "globe"),
    "keep": MonarchStatusDefinition("Keep", tone: .success, systemImage: "checkmark.circle"),
    "let_lapse": MonarchStatusDefinition("Let lapse", tone: .danger, systemImage: "xmark.circle"),
])

private let items = [
    MonarchSavingsItem(
        id: "1",
        name: "old-brand.com",
        category: "parked",
        date: now.addingTimeInterval(86_400 * 10),
        amount: MonarchSavingsAmount(40, currencyCode: "USD"),
        decision: "let_lapse"
    ),
    MonarchSavingsItem(id: "2", name: "campaign-2024.co.uk", category: "no_site", isUnpriced: true, decision: "keep"),
    MonarchSavingsItem(id: "3", name: "A product name long enough to wrap onto a second line on a phone"),
    MonarchSavingsItem(id: "4", name: "spare.dev", amount: MonarchSavingsAmount(15, currencyCode: "EUR")),
]

enum MonarchSavingsPanelFixtures: ComponentFixtures {
    static let id = "savings-panel"
    static let fixtures = [
        Fixture("everything, interactive") {
            MonarchSavingsPanel(
                unusedCount: 9,
                costs: [
                    MonarchSavingsAmount(1_200, currencyCode: "USD"), MonarchSavingsAmount(300, currencyCode: "EUR"),
                ],
                unpricedCount: 2,
                categories: [
                    MonarchSavingsCategory(id: "parked", count: 5),
                    MonarchSavingsCategory(id: "no_site", count: 4),
                    MonarchSavingsCategory(id: "hidden", count: 0),
                ],
                decisions: [
                    MonarchSavingsDecisionCount(id: "keep", label: "Keep", count: 3),
                    MonarchSavingsDecisionCount(id: "let_lapse", label: "Let lapse", count: 4),
                ],
                potentialSavings: [MonarchSavingsAmount(400, currencyCode: "USD")],
                items: items,
                maxItems: 3,
                registry: statuses,
                locale: Locale(identifier: "en_US"),
                calendar: calendar,
                onCategorySelect: { _ in },
                onItemSelect: { _ in },
                actions: { MonarchButton("Review all", size: .sm) {} }
            )
        },
        Fixture("headline only") {
            MonarchSavingsPanel(unusedCount: 1)
        },
        Fixture("read only items") {
            MonarchSavingsPanel(unusedCount: 2, items: Array(items.prefix(2)), registry: statuses, calendar: calendar)
        },
        Fixture("loading") {
            MonarchSavingsPanel(unusedCount: 0, isLoading: true)
        },
        Fixture("empty") {
            MonarchSavingsPanel(unusedCount: 0)
        },
        Fixture("error, retrying") {
            MonarchSavingsPanel(unusedCount: 0, errorMessage: "Couldn’t reach billing.", isRetrying: true, onRetry: {})
        },
    ]
}

@Suite("MonarchSavingsPanel")
struct MonarchSavingsPanelTests {
    private let locale = Locale(identifier: "en_US")

    @Test("An error wins over loading, and nothing unused is the empty state")
    func cardState() {
        #expect(MonarchSavingsPanelTokens.cardState(isLoading: true, errorMessage: "x", unusedCount: 3) == .error)
        #expect(MonarchSavingsPanelTokens.cardState(isLoading: true, errorMessage: nil, unusedCount: 3) == .loading)
        #expect(MonarchSavingsPanelTokens.cardState(isLoading: false, errorMessage: "", unusedCount: 0) == .empty)
        #expect(MonarchSavingsPanelTokens.cardState(isLoading: false, errorMessage: nil, unusedCount: 3) == .ready)
    }

    @Test("Amounts keep their currencies and are never added together")
    func join() {
        let joined = MonarchSavingsPanelTokens.join(
            [MonarchSavingsAmount(1_200, currencyCode: "USD"), MonarchSavingsAmount(300, currencyCode: "EUR")],
            locale: locale
        )
        #expect(joined == "$1,200.00 + €300.00")
        #expect(MonarchSavingsPanelTokens.join([], locale: locale) == nil)
    }

    @Test("The headline counts items, the cost and the unpriced items")
    func headline() {
        #expect(MonarchSavingsPanelStrings.headline(1, cost: nil, unpriced: 0) == "1 unused item.")
        #expect(
            MonarchSavingsPanelStrings.headline(9, cost: "$1,200.00", unpriced: 2)
                == "9 unused items cost $1,200.00 to renew (2 without a price)."
        )
        #expect(
            MonarchSavingsPanelStrings.headline(3, cost: "$5.00", unpriced: 0) == "3 unused items cost $5.00 to renew."
        )
    }

    @Test("Empty categories are hidden and the list is shortened")
    func lists() {
        let shown = MonarchSavingsPanelTokens.shownCategories([
            MonarchSavingsCategory(id: "a", count: 2),
            MonarchSavingsCategory(id: "b", count: 0),
        ])
        #expect(shown.map(\.id) == ["a"])
        let list = MonarchSavingsPanelTokens.shownItems(items, max: 3)
        #expect(list.shown.count == 3)
        #expect(list.hidden == 1)
        #expect(MonarchSavingsPanelTokens.shownItems(items, max: -1).hidden == 4)
    }

    @Test("Decisions are one line")
    func decisions() {
        let line = MonarchSavingsPanelTokens.decisionsLine(
            [
                MonarchSavingsDecisionCount(id: "keep", label: "Keep", count: 3),
                MonarchSavingsDecisionCount(id: "review", label: "Review", count: 1_200),
            ],
            locale: locale
        )
        #expect(line == "Keep 3 · Review 1,200")
        #expect(MonarchSavingsPanelTokens.decisionsLine([], locale: locale) == nil)
    }

    @Test("VoiceOver reads an item's facts after its name")
    func itemValue() {
        let value = MonarchSavingsPanelTokens.itemValue(
            items[0],
            category: "Parked",
            decision: "Let lapse",
            locale: locale,
            calendar: calendar,
            unpriced: "No price"
        )
        #expect(value == "Parked, Nov 24, 2026, $40.00, Let lapse")
        let unpriced = MonarchSavingsPanelTokens.itemValue(
            items[1],
            category: "No website",
            decision: "Keep",
            locale: locale,
            calendar: calendar,
            unpriced: "No price"
        )
        #expect(unpriced == "No website, No price, Keep")
        let bare = MonarchSavingsPanelTokens.itemValue(
            items[2],
            category: nil,
            decision: nil,
            locale: locale,
            calendar: calendar,
            unpriced: "No price"
        )
        #expect(bare.isEmpty)
    }

    @Test("The built-in strings come from the component's table")
    func strings() {
        #expect(MonarchSavingsPanelStrings.title == "Savings")
        #expect(MonarchSavingsPanelStrings.more(4) == "and 4 more")
        #expect(MonarchSavingsPanelStrings.potentialSavings("$4.00") == "Letting the marked items lapse saves $4.00.")
        for value in [
            MonarchSavingsPanelStrings.categories, MonarchSavingsPanelStrings.decisions,
            MonarchSavingsPanelStrings.items, MonarchSavingsPanelStrings.unpriced,
            MonarchSavingsPanelStrings.emptyTitle, MonarchSavingsPanelStrings.emptyMessage,
            MonarchSavingsPanelStrings.errorTitle, MonarchSavingsPanelStrings.loading,
        ] {
            #expect(!value.hasPrefix("monarch."))
        }
    }
}
