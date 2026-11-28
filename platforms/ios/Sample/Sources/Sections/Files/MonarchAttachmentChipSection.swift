import MonarchUI
import SwiftUI

struct MonarchAttachmentChipSection: CataloguePage {
    static let title = "Attachment chip"

    @State private var progress = 0.0
    @State private var status = MonarchUploadStatus.queued
    @State private var isAttached = true

    var body: some View {
        CatalogueCaption("An upload you can start, fail and retry")
        if isAttached {
            MonarchAttachmentChip(
                "Q3 report.pdf",
                size: 2_400_000,
                status: status,
                progress: progress,
                errorMessage: status == .failed ? "The connection was lost" : nil,
                onRetry: { start() },
                onRemove: { isAttached = false }
            )
        } else {
            MonarchButton("Attach again") {
                isAttached = true
                status = .queued
            }
        }
        HStack {
            MonarchButton("Upload", size: .sm) { start() }
            MonarchButton("Fail", variant: .danger, size: .sm) { status = .failed }
        }
        CatalogueCaption("Every status")
        ForEach(MonarchUploadStatus.allCases, id: \.self) { status in
            MonarchAttachmentChip("photo.jpg", size: 800_000, status: status, progress: 0.6, onRemove: {})
        }
    }

    private func start() {
        status = .uploading
        progress = 0
        Task {
            while progress < 1, status == .uploading {
                try? await Task.sleep(for: .milliseconds(200))
                progress += 0.1
            }
            if status == .uploading { status = .done }
        }
    }
}
