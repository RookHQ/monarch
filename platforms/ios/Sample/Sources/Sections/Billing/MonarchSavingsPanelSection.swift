import Foundation
@_spi(Experimental) import MonarchUI
import SwiftUI

struct MonarchSavingsPanelSection: CataloguePage {
    static let title = "Savings panel"

    @State private var message = "Nothing opened"

    private let statuses = MonarchStatusRegistry([
        "parked": MonarchStatusDefinition("Parked", tone: .warning, systemImage: "parkingsign.circle"),
        "no_site": MonarchStatusDefinition("No website", tone: .neutral, systemImage: "globe"),
        "keep": MonarchStatusDefinition("Keep", tone: .success, systemImage: "checkmark.circle"),
        "let_lapse": MonarchStatusDefinition("Let lapse", tone: .danger, systemImage: "xmark.circle"),
    ])

    var body: some View {
        CatalogueCaption("Categories and items open their lists")
        MonarchSavingsPanel(
            unusedCount: 9,
            costs: [MonarchSavingsAmount(1_200, currencyCode: "USD"), MonarchSavingsAmount(300, currencyCode: "EUR")],
            unpricedCount: 2,
            categories: [
                MonarchSavingsCategory(id: "parked", count: 5), MonarchSavingsCategory(id: "no_site", count: 4),
            ],
            decisions: [
                MonarchSavingsDecisionCount(id: "keep", label: "Keep", count: 3),
                MonarchSavingsDecisionCount(id: "let_lapse", label: "Let lapse", count: 4),
            ],
            potentialSavings: [MonarchSavingsAmount(400, currencyCode: "USD")],
            items: [
                MonarchSavingsItem(
                    id: "1",
                    name: "old-brand.com",
                    category: "parked",
                    date: .now.addingTimeInterval(86_400 * 10),
                    amount: MonarchSavingsAmount(40, currencyCode: "USD"),
                    decision: "let_lapse"
                ),
                MonarchSavingsItem(
                    id: "2",
                    name: "campaign-2024.co.uk",
                    category: "no_site",
                    isUnpriced: true,
                    decision: "keep"
                ),
                MonarchSavingsItem(id: "3", name: "spare.dev", amount: MonarchSavingsAmount(15, currencyCode: "EUR")),
            ],
            maxItems: 2,
            registry: statuses,
            onCategorySelect: { message = "Opened \($0.id)" },
            onItemSelect: { message = "Opened \($0.name)" }
        )
        Text(message).monarchTextStyle(.caption)

        CatalogueCaption("Empty, loading and error")
        MonarchSavingsPanel(unusedCount: 0)
        MonarchSavingsPanel(unusedCount: 0, isLoading: true)
        MonarchSavingsPanel(unusedCount: 0, errorMessage: "Couldn’t reach billing.", onRetry: {})
    }
}
