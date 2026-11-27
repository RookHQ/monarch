import MonarchUI
import SwiftUI

struct MonarchButtonGroupSection: CataloguePage {
    static let title = "ButtonGroup"

    @State private var page = 1
    @State private var zoom = 100

    var body: some View {
        ForEach(MonarchButtonVariant.allCases, id: \.self) { variant in
            CatalogueCaption(String(describing: variant))
            MonarchButtonGroup("Pages", variant: variant) {
                Button("Previous") { page = max(page - 1, 1) }
                    .disabled(page == 1)
                Button("Next") { page += 1 }
            }
        }
        CatalogueCaption("Page \(page)")

        CatalogueCaption("Column, icons only")
        MonarchButtonGroup("Zoom", axis: .vertical) {
            Button("Zoom in", systemImage: "plus") { zoom += 10 }
            Button("Zoom out", systemImage: "minus") { zoom = max(zoom - 10, 10) }
        }
        .labelStyle(.iconOnly)
        CatalogueCaption("Zoom: \(zoom)%")

        CatalogueCaption("Full width")
        MonarchButtonGroup("Pages", size: .lg, fullWidth: true) {
            Button("First") { page = 1 }
            Button("Previous") { page = max(page - 1, 1) }
            Button("Next") { page += 1 }
        }

        CatalogueCaption("Not attached")
        MonarchButtonGroup("Pages", isAttached: false) {
            Button("Previous") { page = max(page - 1, 1) }
            Button("Next") { page += 1 }
        }
    }
}
