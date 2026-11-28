import MonarchUI
import SwiftUI

struct MonarchRegionSelectSection: CataloguePage {
    static let title = "Region select"

    @State private var country: String? = "US"
    @State private var region: String?
    /// Counts loads, so the first one fails and "Try again" shows.
    private actor Attempts {
        private var count = 0
        func next() -> Int {
            count += 1
            return count
        }
    }

    private static let attempts = Attempts()

    @State private var loaded: String?

    var body: some View {
        CatalogueCaption("Follows the chosen country")
        MonarchCountrySelect("Country", selection: $country, priority: ["US", "CA", "AU", "GB", "IN", "PT"])
            .onChange(of: country) { _, _ in region = nil }
        MonarchRegionSelect(
            MonarchRegionSelect.fieldLabel(for: country) ?? "Region",
            selection: $region,
            country: country,
            showsCode: true
        )
        Text("Value: \(region ?? "none")").monarchTextStyle(.caption)

        CatalogueCaption("Loaded from a service, failing once")
        MonarchRegionSelect("State", selection: $loaded, country: "US") { country in
            try await Task.sleep(for: .seconds(1))
            if await Self.attempts.next() == 1 { throw URLError(.notConnectedToInternet) }
            return MonarchRegionSelect.builtInRegions(for: country) ?? []
        }

        CatalogueCaption("No country yet")
        MonarchRegionSelect("Region", selection: .constant(nil), country: nil)
    }
}
