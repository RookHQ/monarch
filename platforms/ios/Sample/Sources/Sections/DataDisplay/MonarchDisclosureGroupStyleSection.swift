import MonarchUI
import SwiftUI

struct MonarchDisclosureGroupStyleSection: CataloguePage {
    static let title = "Collapsible"

    @State private var usesProxy = false
    @State private var showsDetails = true

    var body: some View {
        Group {
            DisclosureGroup("Advanced settings") {
                Toggle("Use a proxy", isOn: $usesProxy)
            }
            CatalogueCaption("Expansion held by the app")
            DisclosureGroup("Details", isExpanded: $showsDetails) {
                Text("The content is removed while the section is collapsed.")
                    .monarchTextStyle(.body)
            }
            Toggle("Show details", isOn: $showsDetails)
            CatalogueCaption("Disabled")
            DisclosureGroup("Not available") {
                Text("Content")
            }
            .disabled(true)
        }
        .disclosureGroupStyle(.monarch)
    }
}
