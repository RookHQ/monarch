import MonarchUI
import SwiftUI

struct MonarchReceiptPreviewSection: CataloguePage {
    static let title = "ReceiptPreview"

    @State private var closed = ""

    var body: some View {
        CatalogueCaption("Fetched when pressed (writes a small text file)")
        MonarchReceiptPreview(
            onClose: { url in
                closed = url.lastPathComponent
                try? FileManager.default.removeItem(at: url)
            },
            load: { try await Self.writeSampleReceipt() }
        )
        CatalogueCaption("Fails to load")
        MonarchReceiptPreview(variant: .secondary, size: .md) {
            throw CocoaError(.fileNoSuchFile)
        }
        CatalogueCaption("No receipt")
        MonarchReceiptPreview(url: nil)
        if !closed.isEmpty {
            Text("Closed and deleted: \(closed)").monarchTextStyle(.caption)
        }
    }

    /// Writes a text file in the temporary folder, standing in for the app's
    /// authorised download.
    private static func writeSampleReceipt() async throws -> URL {
        let url = FileManager.default.temporaryDirectory.appendingPathComponent("sample-receipt.txt")
        try Data("Receipt 1042\nCoffee beans, 1 kg\n".utf8).write(to: url)
        return url
    }
}
