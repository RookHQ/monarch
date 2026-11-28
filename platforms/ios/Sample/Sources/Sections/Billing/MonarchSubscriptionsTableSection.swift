import MonarchUI
import SwiftUI

struct MonarchSubscriptionsTableSection: CataloguePage {
    static let title = "SubscriptionsTable"

    @State private var isCustomerShown = true
    @State private var selected = ""

    private let rows = [
        MonarchSubscription(
            "sub_1",
            name: "Acme Pro",
            plan: "Team plan",
            quantity: 3,
            customer: "Acme Inc",
            status: "active",
            amount: 49,
            currencyCode: "USD",
            monthly: .amount(49, currencyCode: "USD"),
            renewalDate: Date(timeIntervalSince1970: 1_775_952_000)
        ),
        MonarchSubscription(
            "sub_2",
            name: "Globex Support",
            customer: "Globex",
            status: "past_due",
            amount: 1_200,
            currencyCode: "EUR",
            interval: .year,
            monthly: .notConverted,
            renewalDate: Date(timeIntervalSince1970: 1_775_952_000),
            renewsAutomatically: false
        ),
        MonarchSubscription(
            "sub_3",
            name: "Initech onboarding",
            plan: "Setup",
            customer: "Initech",
            status: "trial",
            amount: 500,
            currencyCode: "GBP",
            interval: nil
        ),
        MonarchSubscription(
            "sub_4",
            name: "Umbrella analytics",
            plan: "Analytics",
            customer: "Umbrella",
            status: "cancelled",
            amount: 300,
            currencyCode: "USD",
            intervalCount: 3,
            cancelsAt: Date(timeIntervalSince1970: 1_772_582_400)
        ),
    ]

    var body: some View {
        CatalogueCaption("Rows on a phone, a table at regular width; tap a row")
        Toggle("Show customer", isOn: $isCustomerShown)
        MonarchSubscriptionsTable(rows, isCustomerShown: isCustomerShown) { subscription in
            selected = subscription.name
        }
        if !selected.isEmpty {
            Text("Opened: \(selected)").monarchTextStyle(.caption)
        }

        CatalogueCaption("No subscriptions")
        MonarchSubscriptionsTable([])
    }
}
