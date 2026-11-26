import MonarchUI
import SwiftUI

struct MonarchPhoneInputSection: CataloguePage {
    static let title = "Phone input"

    @State private var mobile: String? = "+442079460958"
    @State private var work: String?
    @State private var lastCountry = "PT"

    var body: some View {
        CatalogueCaption("One E.164 value out, written when the field loses focus")
        MonarchPhoneInput(
            "Mobile number",
            value: $mobile,
            helper: "Paste +351 912 345 678 to change the country.",
            preferred: ["GB", "PT", "US"]
        )
        Text("Value: \(mobile ?? "none")")
            .monarchTextStyle(.caption)
        if let mobile, let parts = MonarchPhoneInput.parse(mobile) {
            Text("Country \(parts.country), calling code \(parts.dialCode), national \(parts.national)")
                .monarchTextStyle(.caption)
        }

        CatalogueCaption("Required, with a default country and a country callback")
        MonarchPhoneInput(
            "Work phone",
            value: $work,
            prompt: "912 345 678",
            defaultCountry: "PT",
            isRequired: true,
            onCountryChange: { lastCountry = $0 }
        )
        Text("Country: \(lastCountry)")
            .monarchTextStyle(.caption)

        CatalogueCaption("Disabled, without flags")
        MonarchPhoneInput("Office", value: .constant("+12025550100"), showsFlags: false)
            .disabled(true)
    }
}
