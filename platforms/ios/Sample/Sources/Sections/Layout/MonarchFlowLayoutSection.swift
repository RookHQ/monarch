import MonarchUI
import SwiftUI

struct MonarchFlowLayoutSection: CataloguePage {
    static let title = "Inline (flow layout)"

    @Environment(\.monarchTheme) private var theme
    @State private var tags = ["Design", "Research", "Engineering", "Customer support", "Sales", "Finance"]

    var body: some View {
        CatalogueCaption("Tags that wrap")
        MonarchFlowLayout(spacing: theme.spacing.space2) {
            ForEach(tags, id: \.self) { tag in
                MonarchBadge(tag, tone: .info)
            }
        }
        MonarchButton("Add a tag", systemImage: "plus", size: .sm) {
            tags.append("Tag \(tags.count + 1)")
        }

        ForEach(MonarchFlowLayoutJustification.allCases, id: \.self) { justification in
            CatalogueCaption(String(describing: justification))
            MonarchFlowLayout(
                spacing: theme.spacing.space2,
                rowSpacing: theme.spacing.space1,
                justification: justification
            ) {
                MonarchButton("Save", variant: .primary) {}
                MonarchButton("Save as draft") {}
                MonarchButton("Discard", variant: .ghost) {}
            }
        }

        CatalogueCaption("Aligned on the first baseline")
        MonarchFlowLayout(spacing: theme.spacing.space3, alignment: .firstTextBaseline) {
            Text("Invoice 1042").monarchTextStyle(.heading)
            MonarchBadge("Paid", tone: .success)
            Text("Due 4 March").monarchTextStyle(.caption)
        }
    }
}
