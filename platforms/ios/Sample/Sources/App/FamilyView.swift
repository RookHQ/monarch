import SwiftUI

/// The pages of one family.
struct FamilyView: View {
    let family: CatalogueFamily

    var body: some View {
        List(family.entries) { entry in
            NavigationLink(entry.title) {
                CataloguePageContainer(title: entry.title) { entry.make() }
            }
        }
        .navigationTitle(family.name)
        .navigationBarTitleDisplayMode(.inline)
    }
}
