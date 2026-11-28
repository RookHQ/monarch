import MonarchUI
import SwiftUI

struct MonarchAvatarUploadSection: CataloguePage {
    static let title = "Avatar upload"

    @State private var picture: Image?
    @State private var savedBytes: Int?
    @State private var failsNextSave = false

    var body: some View {
        CatalogueCaption("A person: circle crop")
        MonarchAvatarUpload(
            name: "Ada Lovelace",
            image: picture,
            maxFileSize: 10_000_000,
            onRemove: {
                picture = nil
                savedBytes = nil
            },
            onSave: { result in
                if failsNextSave {
                    failsNextSave = false
                    throw CancellationError()
                }
                savedBytes = result.data.count
                picture = Image(systemName: "person.crop.circle.fill")
            }
        )
        Toggle("Make the next save fail", isOn: $failsNextSave)
            .toggleStyle(.monarchCheckbox)
        if let savedBytes {
            CatalogueCaption("Saved a \(savedBytes.formatted(.byteCount(style: .file))) PNG")
        }
        CatalogueCaption("A workspace: square crop, JPEG")
        MonarchAvatarUpload(
            name: "Rook Labs",
            image: nil,
            shape: .square,
            outputSize: 256,
            outputFormat: .jpeg(quality: 0.85)
        ) { _ in }
        CatalogueCaption("Disabled")
        MonarchAvatarUpload(name: "Grace Hopper", url: nil) { _ in }
            .disabled(true)
    }
}
