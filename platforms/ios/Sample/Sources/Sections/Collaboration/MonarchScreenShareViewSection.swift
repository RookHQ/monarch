import MonarchUI
import SwiftUI

struct MonarchScreenShareViewSection: CataloguePage {
    static let title = "Screen share view"

    @State private var zoom = 1.0
    @State private var isInPictureInPicture = false
    @State private var isFullScreen = false
    @State private var isSharing = true

    var body: some View {
        MonarchScreenShareView(
            "Ana Flores",
            zoom: $zoom,
            isPictureInPictureActive: isInPictureInPicture,
            isFullScreen: isFullScreen,
            onPictureInPicture: { isInPictureInPicture.toggle() },
            onFullScreen: { isFullScreen.toggle() },
            video: { picture }
        )
        Text("Zoom held by the page: \(zoom.formatted(.percent))").monarchTextStyle(.caption)
        Text("The picture-in-picture and full-screen buttons only switch their state here; a product starts them.")
            .monarchTextStyle(.caption)

        CatalogueCaption("Waiting for the picture")
        MonarchScreenShareView("Ana Flores", isWaiting: true) { EmptyView() }

        CatalogueCaption("You are presenting")
        if isSharing {
            MonarchScreenShareView("You", isLocal: true, onStopSharing: { isSharing = false }, video: { picture })
        } else {
            Button("Share again") { isSharing = true }
                .buttonStyle(.monarch(.secondary, size: .sm))
        }
    }

    /// Stands in for the video view of a call SDK.
    private var picture: some View {
        Image(systemName: "macwindow")
            .resizable()
            .scaledToFit()
            .accessibilityHidden(true)
    }
}
