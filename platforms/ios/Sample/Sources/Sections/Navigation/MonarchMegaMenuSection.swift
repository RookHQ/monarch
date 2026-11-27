import MonarchUI
import SwiftUI

struct MonarchMegaMenuSection: CataloguePage {
    static let title = "Mega menu"

    @State private var chosen = ""

    var body: some View {
        CatalogueCaption("Tap Product")
        MonarchMegaMenu(items: [
            MonarchMegaMenuItem(
                "product",
                label: "Product",
                groups: [
                    MonarchMegaMenuGroup(
                        "Plan",
                        links: [
                            MonarchMegaMenuLink(
                                "Projects",
                                description: "Track work from idea to launch",
                                systemImage: "square.grid.2x2"
                            ),
                            MonarchMegaMenuLink(
                                "Calendar",
                                description: "Deadlines and milestones in one view",
                                systemImage: "calendar",
                                badge: "New"
                            ),
                        ]
                    )
                ],
                featured: MonarchMegaMenuLink(
                    "What changed this month",
                    description: "A short tour of the newest features and fixes.",
                    systemImage: "square.grid.2x2",
                    badge: "Updated"
                )
            ),
            MonarchMegaMenuItem("support", label: "Support", destination: URL(string: "https://example.com/support")),
        ]) { link in
            chosen = link.label
        }
        Text(chosen.isEmpty ? "Nothing chosen" : "Chose \(chosen)").monarchTextStyle(.caption)
    }
}
