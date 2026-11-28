import MonarchUI
import SwiftUI

struct MonarchMediaUploadSection: CataloguePage {
    static let title = "MediaUpload"

    /// Stands in for the app's storage: reports progress, then keeps the local file.
    private static func store(
        _ file: MonarchFileUploadFile,
        _ context: MonarchMediaUploadContext
    ) async throws -> MonarchMediaUploadResult {
        for step in 1...5 {
            try await Task.sleep(for: .milliseconds(250))
            context.reportProgress(Double(step) / 5)
        }
        return MonarchMediaUploadResult(url: file.url)
    }

    /// An error with its own message, shown as it is.
    private struct StorageFull: LocalizedError {
        var errorDescription: String? { "Storage is full. Remove an old file and try again." }
    }

    @State private var cover: URL?
    @State private var tile: URL?
    @State private var banner: URL?

    var body: some View {
        CatalogueCaption("An image, videos refused with a reason")
        MonarchMediaUpload(
            "Cover image",
            url: $cover,
            helper: "Shown at the top of the page.",
            maxImageSize: 10_000_000,
            videoUnavailable: "Videos can’t be added to this item.",
            onUpload: Self.store
        )

        CatalogueCaption("An image or a video, with limits")
        MonarchMediaUpload(
            "Tile media",
            url: $tile,
            kinds: [.image, .video],
            maxImageSize: 10_000_000,
            maxVideoSize: 20_000_000,
            maxVideoDuration: 90,
            onUpload: Self.store
        )

        CatalogueCaption("An upload that fails")
        MonarchMediaUpload("Profile banner", url: $banner) { _, _ in
            try await Task.sleep(for: .milliseconds(600))
            throw StorageFull()
        }
    }
}
