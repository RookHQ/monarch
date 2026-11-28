import MonarchUI
import SwiftUI

struct MonarchCardSection: CataloguePage {
    static let title = "Card"

    var body: some View {
        ForEach(MonarchElevationLevel.allCases, id: \.self) { level in
            MonarchCard(elevation: level) {
                Text("Elevation \(String(describing: level))").monarchTextStyle(.subheading)
                Text("A card groups related content.").monarchTextStyle(.body)
            }
        }

        CatalogueCaption("Flush content with a footer")
        MonarchCard(padding: .flush) {
            ForEach(["Invoice 1042", "Invoice 1043"], id: \.self) { title in
                Text(title).monarchTextStyle(.body).padding()
                MonarchDivider()
            }
        } footer: {
            MonarchButton("View all", variant: .ghost, size: .sm) {}
        }
    }
}
