import MonarchUI
import SwiftUI

/// The root of the catalogue: appearance, foundations, then one row per
/// family. The families and their pages come from the generated
/// `CatalogueRegistry`.
struct CatalogueView: View {
    var body: some View {
        NavigationStack {
            List {
                Section("Appearance") {
                    AppearanceControls()
                }
                Section("Foundations") {
                    NavigationLink("Typography") {
                        CataloguePageContainer(title: "Typography") { TypographyPage() }
                    }
                    NavigationLink("Colour roles") {
                        CataloguePageContainer(title: "Colour roles") { ColourPage() }
                    }
                }
                Section("Components") {
                    ForEach(CatalogueRegistry.families) { family in
                        NavigationLink {
                            FamilyView(family: family)
                        } label: {
                            LabeledContent(family.name, value: "\(family.entries.count)")
                        }
                    }
                }
            }
            .navigationTitle("Monarch")
        }
    }
}
