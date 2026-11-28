import MonarchUI
import SwiftUI

struct MonarchVideoThumbnailSection: CataloguePage {
    static let title = "Video thumbnail"

    @State private var playCount = 0

    private let poster = Image(systemName: "mountain.2.fill")

    var body: some View {
        CatalogueCaption("A button with a poster and a length. Played \(playCount) times.")
        MonarchVideoThumbnail("Product tour", image: poster, duration: 65) {
            playCount += 1
        }

        CatalogueCaption("No poster: the placeholder")
        MonarchVideoThumbnail("Keynote", image: nil, duration: 3725)

        CatalogueCaption("Square, letterboxed, without the play mark")
        MonarchVideoThumbnail("Portrait clip", image: poster, aspectRatio: 1, contentMode: .fit, showsPlayIcon: false)
            .frame(maxWidth: 200)

        CatalogueCaption("Disabled")
        MonarchVideoThumbnail("Locked", image: poster, duration: 12) {}
            .disabled(true)
    }
}
