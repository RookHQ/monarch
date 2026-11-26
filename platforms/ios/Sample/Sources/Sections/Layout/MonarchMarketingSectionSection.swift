import MonarchUI
import SwiftUI

struct MonarchMarketingSectionSection: CataloguePage {
    static let title = "Marketing sections"

    var body: some View {
        CatalogueCaption("Section with features")
        MonarchMarketingSection(
            "Every idea gets the right shape.",
            eyebrow: "The editor",
            description: "Type a slash and pick a block."
        ) {
            MonarchFeatureGrid(items: [
                MonarchFeatureGridItem(
                    "Slash commands",
                    description: "Tables, lists and callouts, one keystroke away."
                ),
                MonarchFeatureGridItem("Version history", description: "Named snapshots you can restore safely."),
            ])
        }

        CatalogueCaption("Product frame")
        MonarchProductFrame("Share dialog with a copyable link", chrome: .browser, title: "example.rookhq.com/share") {
            Text("Anyone with the link can view").padding()
        }

        CatalogueCaption("Stats")
        MonarchStatStrip(items: [
            MonarchStatStripItem("3", label: "Export formats"),
            MonarchStatStripItem("13", label: "Interface languages"),
        ])

        CatalogueCaption("FAQ")
        MonarchFaq(items: [MonarchFaqItem("Which formats can I export?", answer: "HTML, Markdown and PDF.")])

        CatalogueCaption("Call to action")
        MonarchCtaBand("Start with a blank page.") {
            MonarchButton("Start writing", variant: .primary) {}
        }
    }
}
