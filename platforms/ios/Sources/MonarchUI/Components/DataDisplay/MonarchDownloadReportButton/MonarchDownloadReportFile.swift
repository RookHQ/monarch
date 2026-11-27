import SwiftUI
import UniformTypeIdentifiers

/// A file the app prepared at a local URL, handed to the system save dialog
/// (`fileExporter`). The content type comes from the file extension.
struct MonarchDownloadReportFile: FileDocument {
    static let readableContentTypes: [UTType] = [.data]

    let url: URL

    init(url: URL) {
        self.url = url
    }

    init(configuration: ReadConfiguration) throws {
        throw CocoaError(.fileReadUnsupportedScheme)
    }

    /// The content type of the file, from its extension, or plain data.
    var contentType: UTType {
        UTType(filenameExtension: url.pathExtension) ?? .data
    }

    func fileWrapper(configuration: WriteConfiguration) throws -> FileWrapper {
        try FileWrapper(url: url)
    }
}
