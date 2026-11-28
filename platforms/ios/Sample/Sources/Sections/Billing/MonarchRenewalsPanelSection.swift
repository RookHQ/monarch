import Foundation
@_spi(Experimental) import MonarchUI
import SwiftUI

struct MonarchRenewalsPanelSection: CataloguePage {
    static let title = "Renewals panel"

    @State private var window = "30"
    @State private var message = "Nothing opened"
    @State private var isLoading = false

    private var items: [MonarchRenewalItem] {
        let all = [
            MonarchRenewalItem(
                id: "acme",
                name: "Acme support contract",
                status: "open",
                renewalDate: .now.addingTimeInterval(86_400 * 6),
                description: "Support · Acme Inc",
                amount: 12_000,
                currencyCode: "USD",
                amountNote: "per year",
                autoRenews: false,
                probability: 60,
                owner: "Priya",
                nextAction: "Send a quote",
                nextActionDate: .now.addingTimeInterval(-86_400),
                isNextActionOverdue: true
            ),
            MonarchRenewalItem(
                id: "domain",
                name: "example.com",
                status: "in_progress",
                renewalDate: .now.addingTimeInterval(86_400 * 24),
                noticeDeadline: .now.addingTimeInterval(86_400 * 14),
                amount: 40,
                currencyCode: "EUR",
                autoRenews: true,
                owner: "Sam"
            ),
            MonarchRenewalItem(
                id: "licence",
                name: "Design licence",
                status: "renewed",
                renewalDate: .now.addingTimeInterval(86_400 * 70),
                amount: 2_400,
                currencyCode: "USD",
                outcome: "Renewed for two years"
            ),
        ]
        return window == "30" ? all.filter { $0.id != "licence" } : all
    }

    var body: some View {
        CatalogueCaption("Filters, totals, row actions")
        MonarchRenewalsPanel(
            items,
            description: "What renews or expires in the selected window.",
            filters: [
                MonarchRenewalFilter(value: "30", label: "30 days", count: 2),
                MonarchRenewalFilter(value: "90", label: "90 days", count: 3),
            ],
            filter: $window,
            totals: [MonarchRenewalTotal(currencyCode: "USD", amount: 12_000, count: 1, estimated: 2_000)],
            unpricedCount: 0,
            isLoading: isLoading,
            onSelect: { message = "Opened \($0.name)" },
            onAction: { message = "Update \($0.name)" },
            canAct: { $0.status != "renewed" },
            actions: {
                Button("Check now") { message = "Checked" }
                    .buttonStyle(.monarch(.ghost, size: .sm))
            }
        )
        Text(message).monarchTextStyle(.caption)
        Toggle("Loading", isOn: $isLoading)

        CatalogueCaption("Empty and error")
        MonarchRenewalsPanel([])
        MonarchRenewalsPanel([], errorMessage: "The renewals service is down.", onRetry: {})
    }
}
