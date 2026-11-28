import MonarchUI
import SwiftUI

struct MonarchUploadQueueSection: CataloguePage {
    static let title = "Upload queue"

    @State private var items = [
        MonarchUploadQueueItem(id: "1", name: "Q3 report.pdf", size: 2_400_000, status: .done),
        MonarchUploadQueueItem(id: "2", name: "photo.jpg", size: 800_000, status: .uploading, progress: 0.4),
        MonarchUploadQueueItem(id: "3", name: "notes.txt", size: 12_000, status: .queued),
        MonarchUploadQueueItem(id: "4", name: "big.mov", size: 900_000_000, status: .failed, errorMessage: "Too large"),
    ]

    var body: some View {
        MonarchUploadQueue(
            items,
            onCancel: { id in update(id) { replaced($0, status: .cancelled) } },
            onRetry: { id in update(id) { replaced($0, status: .uploading, progress: 0.1) } },
            onRemove: { id in items.removeAll { $0.id == id } },
            onClearFinished: { items.removeAll { !$0.status.isActive } }
        )
        MonarchButton("Finish the active uploads") {
            items = items.map { $0.status.isActive ? replaced($0, status: .done) : $0 }
        }
    }

    private func update(_ id: String, _ change: (MonarchUploadQueueItem) -> MonarchUploadQueueItem) {
        items = items.map { $0.id == id ? change($0) : $0 }
    }

    private func replaced(
        _ item: MonarchUploadQueueItem,
        status: MonarchUploadStatus,
        progress: Double? = nil
    ) -> MonarchUploadQueueItem {
        MonarchUploadQueueItem(id: item.id, name: item.name, size: item.size, status: status, progress: progress)
    }
}
