@_spi(Experimental) import MonarchUI
import SwiftUI

struct MonarchAddressAutocompleteSection: CataloguePage {
    static let title = "AddressAutocomplete"

    /// Stands in for the app's own address provider.
    private nonisolated static let places = [
        MonarchAddressAutocompleteSuggestion(
            id: "lisbon",
            label: "10 Rua Augusta",
            description: "Lisbon, Portugal",
            address: MonarchAddressAutocompleteAddress(
                line1: "10 Rua Augusta",
                city: "Lisbon",
                region: "Lisboa",
                postalCode: "1100-053",
                countryCode: "PT"
            )
        ),
        MonarchAddressAutocompleteSuggestion(
            id: "berlin",
            label: "10 Unter den Linden",
            description: "Berlin, Germany"
        ),
    ]

    @State private var shipping = MonarchAddressAutocompleteAddress()
    @State private var billing = MonarchAddressAutocompleteAddress(
        line1: "1 Infinite Loop",
        city: "Cupertino",
        region: "CA",
        postalCode: "95014",
        countryCode: "US"
    )

    var body: some View {
        CatalogueCaption("Search, then review")
        MonarchAddressAutocomplete(
            "Shipping address",
            address: $shipping,
            helper: "Try “10”.",
            isRequired: true,
            minQueryLength: 2,
            search: { query in
                try await Task.sleep(for: .milliseconds(300))
                return Self.places.filter { $0.label.localizedStandardContains(query) }
            },
            resolve: { suggestion in
                try await Task.sleep(for: .milliseconds(300))
                return MonarchAddressAutocompleteAddress(
                    line1: suggestion.label,
                    city: "Berlin",
                    postalCode: "10117",
                    countryCode: "DE"
                )
            }
        )

        CatalogueCaption("Filled in")
        MonarchAddressAutocomplete("Billing address", address: $billing, search: { _ in [] })

        CatalogueCaption("Disabled")
        MonarchAddressAutocomplete(address: .constant(billing), mode: .constant(.search), search: { _ in [] })
            .disabled(true)
    }
}
