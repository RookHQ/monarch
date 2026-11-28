import MonarchUI
import SwiftUI

struct MonarchLinkCardSection: CataloguePage {
    static let title = "LinkCard"

    @State private var isShown = true

    private let url = URL(string: "https://example.com/blog/march-release") ?? URL(fileURLWithPath: "/")
    private let pricing = URL(string: "https://example.com/pricing") ?? URL(fileURLWithPath: "/")

    var body: some View {
        CatalogueCaption("Medium, with a picture")
        if isShown {
            MonarchLinkCard(
                "Release notes for March",
                url: url,
                description: "Faster search, shared views and a new API.",
                siteName: "Example blog",
                image: Image(systemName: "photo.on.rectangle"),
                imageSize: CGSize(width: 1200, height: 630)
            ) {
                isShown = false
            }
        } else {
            MonarchButton("Show the preview again", variant: .ghost, size: .sm) { isShown = true }
        }
        CatalogueCaption("Video")
        MonarchLinkCard(
            "A tour of shared views",
            url: url,
            image: Image(systemName: "film"),
            isVideo: true
        )
        CatalogueCaption("Small")
        MonarchLinkCard(
            "Pricing",
            url: pricing,
            description: "Plans for teams of every size.",
            image: Image(systemName: "photo"),
            size: .sm
        )
        CatalogueCaption("Loading placeholder (no picture yet)")
        MonarchLinkCard("Help centre", url: url, image: nil)
            .redacted(reason: .placeholder)
            .accessibilityHidden(true)
    }
}
