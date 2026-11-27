import MonarchUI
import SwiftUI

struct MonarchEmbedBlockSection: CataloguePage {
    static let title = "Embed block"

    @State private var pasted = "https://vimeo.com/76979871"

    var body: some View {
        CatalogueCaption("A video, loaded after a tap")
        MonarchEmbedBlock(
            url: "https://www.youtube.com/watch?v=aqz-KE-bpKQ",
            caption: "Launch walkthrough",
            requiresTapToLoad: true
        )
        CatalogueCaption("Paste a link")
        MonarchTextField("Link to embed", text: $pasted, prompt: "https://")
        MonarchEmbedBlock(url: pasted, requiresTapToLoad: true)
        CatalogueCaption("Any other site is a link card")
        MonarchEmbedBlock(url: "https://www.example.com/notes", caption: "Meeting notes")
        CatalogueCaption("A link that cannot be shown")
        MonarchEmbedBlock(url: "javascript:alert(1)")
    }
}
