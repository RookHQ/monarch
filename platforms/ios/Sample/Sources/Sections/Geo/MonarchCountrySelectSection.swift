import MonarchUI
import SwiftUI

struct MonarchCountrySelectSection: CataloguePage {
    static let title = "Country select"

    @State private var country: String?
    @State private var shipTo: String? = "GB"
    @State private var french: String? = "DE"

    var body: some View {
        CatalogueCaption("Every country, with suggestions, required")
        MonarchCountrySelect(
            "Country",
            selection: $country,
            prompt: "Choose a country",
            error: country == nil ? "Choose a country." : nil,
            priority: ["US", "GB", "IN"],
            isRequired: true
        )
        Text("Value: \(country ?? "none")").monarchTextStyle(.caption)

        CatalogueCaption("Limited list, no flags")
        MonarchCountrySelect(
            "Ship to",
            selection: $shipTo,
            helper: "We ship to these countries.",
            countries: ["GB", "IE", "FR", "DE", "ES", "PT", "NL", "BE"],
            showsFlag: false
        )

        CatalogueCaption("Names in French")
        MonarchCountrySelect("Pays", selection: $french, locale: Locale(identifier: "fr_FR"))

        CatalogueCaption("Disabled")
        MonarchCountrySelect("Country", selection: .constant("JP")).disabled(true)
    }
}
