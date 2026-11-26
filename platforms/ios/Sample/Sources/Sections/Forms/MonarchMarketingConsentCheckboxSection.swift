import MonarchUI
import SwiftUI

struct MonarchMarketingConsentCheckboxSection: CataloguePage {
    static let title = "Marketing consent checkbox"

    @State private var single: Set<MonarchMarketingChannel> = []
    @State private var several: Set<MonarchMarketingChannel> = []
    @State private var record: MonarchMarketingConsent?

    var body: some View {
        CatalogueCaption("One channel")
        MonarchMarketingConsentCheckbox(
            selection: $single,
            description: "Product updates and offers, about twice a month.",
            policyTitle: "Privacy policy",
            policyURL: URL(string: "https://example.com/privacy")
        ) { consent in
            record = consent
        }

        CatalogueCaption("Several channels")
        MonarchMarketingConsentCheckbox(
            selection: $several,
            channels: MonarchMarketingChannel.allCases
        ) { consent in
            record = consent
        }

        CatalogueCaption("The record of the last change")
        if let record {
            Text(record.text).monarchTextStyle(.caption)
            Text(
                record.granted.isEmpty
                    ? "No channel granted"
                    : "Granted: " + record.granted.map(\.rawValue).sorted().joined(separator: ", ")
            )
            .monarchTextStyle(.caption)
        } else {
            Text("Tick a box to see what is recorded.").monarchTextStyle(.caption)
        }

        CatalogueCaption("Disabled")
        MonarchMarketingConsentCheckbox(selection: .constant([]))
            .disabled(true)
    }
}
