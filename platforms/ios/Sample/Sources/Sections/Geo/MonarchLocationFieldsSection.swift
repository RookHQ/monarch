import MonarchUI
import SwiftUI

struct MonarchLocationFieldsSection: CataloguePage {
    static let title = "Location fields"

    @State private var address = MonarchLocationValue(country: "US")
    @State private var searched = MonarchLocationValue(country: "US")

    /// A small in-memory city source standing in for the app's service.
    private static let adapter = MonarchLocationAdapter(
        cities: { query in
            try await Task.sleep(for: .milliseconds(400))
            let all = [
                MonarchLocationCity(
                    id: "sf",
                    name: "San Francisco",
                    description: "California",
                    region: MonarchRegion(code: "US-CA", name: "California")
                ),
                MonarchLocationCity(
                    id: "oak",
                    name: "Oakland",
                    description: "California",
                    region: MonarchRegion(code: "US-CA", name: "California")
                ),
                MonarchLocationCity(
                    id: "aus",
                    name: "Austin",
                    description: "Texas",
                    region: MonarchRegion(code: "US-TX", name: "Texas")
                ),
                MonarchLocationCity(
                    id: "sa",
                    name: "San Antonio",
                    description: "Texas",
                    region: MonarchRegion(code: "US-TX", name: "Texas")
                ),
            ]
            return all.filter { city in
                (query.region == nil || city.region?.code == query.region)
                    && (query.query.isEmpty || city.name.localizedCaseInsensitiveContains(query.query))
            }
        }
    )

    var body: some View {
        CatalogueCaption("Built-in lists and text fields")
        MonarchLocationFields(
            "Shipping address",
            value: $address,
            required: [.country, .city],
            priorityCountries: ["US", "CA", "GB", "AU", "IN"]
        )
        Text("Value: \(describe(address))").monarchTextStyle(.caption)

        CatalogueCaption("City search from an adapter")
        MonarchLocationFields(value: $searched, adapter: Self.adapter, fields: [.country, .region, .city])
        Text("Value: \(describe(searched))").monarchTextStyle(.caption)

        CatalogueCaption("Disabled")
        MonarchLocationFields(
            value: .constant(
                MonarchLocationValue(country: "GB", region: "GB-SCT", regionName: "Scotland", city: "Edinburgh")
            )
        )
        .disabled(true)
    }

    private func describe(_ value: MonarchLocationValue) -> String {
        [value.country ?? "", value.region ?? value.regionName, value.cityID ?? value.city, value.postalCode]
            .filter { !$0.isEmpty }
            .joined(separator: " · ")
    }
}
