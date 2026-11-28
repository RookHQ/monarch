import MonarchUI
import SwiftUI

struct MonarchMarketingHeroSection: CataloguePage {
    static let title = "Marketing hero"

    var body: some View {
        CatalogueCaption("Centred")
        MonarchMarketingHero(
            "Write it once. Find it whenever.",
            eyebrow: "Rook Example",
            lead: "Structured documents for teams whose decisions need to stay connected.",
            note: "Set up in under five minutes."
        ) {
            MonarchButton("Start writing", variant: .primary) {}
            MonarchButton("See how it works") {}
        }

        CatalogueCaption("Leading, without the backdrop")
        MonarchMarketingHero(
            "Write it once. Find it whenever.",
            lead: "Structured documents for teams whose decisions need to stay connected.",
            alignment: .leading,
            showsBackdrop: false
        )
    }
}
