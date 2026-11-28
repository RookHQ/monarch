import MonarchUI
import SwiftUI

struct MonarchFileUploadSection: CataloguePage {
    static let title = "File upload"

    @State private var attached: [MonarchFileUploadFile] = []
    @State private var avatar: MonarchFileUploadFile?

    var body: some View {
        CatalogueCaption("Up to 5 files of 10 MB, files or photos")
        MonarchFileUpload(
            maxSize: 10_000_000,
            maxFiles: 5,
            currentCount: attached.count,
            allowsPhotos: true
        ) { accepted, _ in
            attached.append(contentsOf: accepted)
        }
        ForEach(attached, id: \.url) { file in
            MonarchFileCard(file.name, size: file.size, layout: .row)
        }
        if !attached.isEmpty {
            Button("Remove all") { attached = [] }
                .buttonStyle(.monarch(.ghost, size: .sm))
        }

        CatalogueCaption("Compact, one image, required")
        MonarchFileUpload(
            allowedContentTypes: [.image],
            maxSize: 2_000_000,
            maxFiles: 1,
            variant: .compact,
            allowsPhotos: true,
            isInvalid: avatar == nil
        ) { accepted, _ in
            avatar = accepted.first
        }
        Text(avatar.map { "Chosen: \($0.name)" } ?? "No picture chosen.").monarchTextStyle(.caption)

        CatalogueCaption("Disabled")
        MonarchFileUpload { _, _ in }.disabled(true)
    }
}
