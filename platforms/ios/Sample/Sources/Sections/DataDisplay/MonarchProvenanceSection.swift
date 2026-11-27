import MonarchUI
import SwiftUI

struct MonarchProvenanceSection: CataloguePage {
    static let title = "Provenance"

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        CatalogueCaption("Chip next to a value")
        HStack(spacing: theme.spacing.space2) {
            Text("Acme Ltd").monarchTextStyle(.bodyStrong)
            MonarchProvenance("Registry", checkedAt: .now.addingTimeInterval(-7200))
        }
        CatalogueCaption("Inline, with an icon")
        MonarchProvenance(
            "Imported file",
            checkedAt: .now.addingTimeInterval(-86_400),
            systemImage: "doc",
            variant: .inline
        )
        CatalogueCaption("Opens the source")
        MonarchProvenance(
            "Companies House",
            checkedAt: .now.addingTimeInterval(-300),
            systemImage: "building.columns",
            url: URL(string: "https://example.com")
        )
    }
}
