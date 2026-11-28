import MonarchUI
import QuickLook
import SwiftUI

/// Native adoption: Quick Look shows the file; Monarch supplies the row around it.
struct FilePreviewSection: CataloguePage {
    static let title = "File preview (Quick Look)"

    @State private var previewURL: URL?
    @State private var failure: String?

    private let fileName = "monarch-sample-notes.txt"

    private var fileURL: URL {
        FileManager.default.temporaryDirectory.appendingPathComponent(fileName)
    }

    var body: some View {
        CatalogueCaption("A file row that opens Quick Look")
        HStack {
            MonarchFileTypeIcon(fileName: fileName, size: .lg, isDecorative: true)
            Text(fileName).monarchTextStyle(.body)
            Spacer()
            MonarchButton("Preview", systemImage: "eye") { openPreview() }
        }
        .quickLookPreview($previewURL)

        if let failure {
            MonarchBanner(message: failure, tone: .danger)
        }

        CatalogueCaption("A type Quick Look cannot show: share or save it instead")
        HStack {
            MonarchFileTypeIcon(fileName: "backup.zip", size: .lg, isDecorative: true)
            Text("backup.zip").monarchTextStyle(.body)
            Spacer()
            ShareLink(item: fileURL) {
                Label("Share", systemImage: "square.and.arrow.up")
            }
            .buttonStyle(.monarch(.secondary))
        }
    }

    /// Writes a small text file to the temporary directory, then presents it.
    private func openPreview() {
        do {
            try "Quick Look shows this text file.\nImages, PDF, video and audio open the same way."
                .write(to: fileURL, atomically: true, encoding: .utf8)
            failure = nil
            previewURL = fileURL
        } catch {
            failure = "The sample file could not be written."
        }
    }
}
