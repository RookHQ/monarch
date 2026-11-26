import MonarchUI
import SwiftUI

struct MonarchAiAttachmentTraySection: CataloguePage {
    static let title = "AI attachment tray"

    @State private var files = Self.start
    @State private var isUploading = false

    private static let start = [
        MonarchAiAttachmentTrayFile(id: "a", name: "Q3 report.pdf", size: 2_400_000, contentType: "application/pdf"),
        MonarchAiAttachmentTrayFile(id: "b", name: "photo.png", size: 840_000, contentType: "image/png", progress: 0),
        MonarchAiAttachmentTrayFile(id: "c", name: "data.csv", size: 31_000_000, errorMessage: "File too large"),
    ]

    var body: some View {
        CatalogueCaption("Upload, retry and remove")
        MonarchAiAttachmentTray(
            files,
            onRetry: { id in upload(id) },
            onRemove: { id in files.removeAll { $0.id == id } }
        )
        MonarchButton("Reset", variant: .secondary, size: .sm) {
            files = Self.start
            upload("b")
        }
        .onAppear { upload("b") }
    }

    private func upload(_ id: String) {
        guard let index = files.firstIndex(where: { $0.id == id }) else { return }
        files[index].errorMessage = nil
        files[index].isFailed = false
        files[index].progress = 0
        Task {
            for step in 1...10 {
                try? await Task.sleep(for: .milliseconds(200))
                guard let index = files.firstIndex(where: { $0.id == id }) else { return }
                files[index].progress = Double(step) / 10
            }
        }
    }
}
