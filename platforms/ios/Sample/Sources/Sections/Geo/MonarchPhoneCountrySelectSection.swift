import MonarchUI
import SwiftUI

struct MonarchPhoneCountrySelectSection: CataloguePage {
    static let title = "Phone country select"

    @State private var country: String? = "PT"
    @State private var number = "+44 20 7946 0000"
    @State private var limited: String?

    var body: some View {
        CatalogueCaption("Every calling code, with suggestions")
        MonarchPhoneCountrySelect("Country code", selection: $country, preferred: ["PT", "ES", "GB"])
        Text("Calling code: \(country.flatMap { MonarchPhoneCountry.country(code: $0)?.dialCode } ?? "none")")
            .monarchTextStyle(.caption)

        CatalogueCaption("Detected from a typed number")
        MonarchTextField("Phone number", text: $number)
        Text("Belongs to: \(MonarchPhoneCountry.matching(number)?.code ?? "unknown")")
            .monarchTextStyle(.caption)

        CatalogueCaption("Limited list, clearable, no flags")
        MonarchPhoneCountrySelect(
            "Country code",
            selection: $limited,
            prompt: "Choose a country",
            countries: ["US", "CA", "GB", "IE"],
            showsFlags: false,
            isRequired: true,
            isClearable: true
        )
    }
}
