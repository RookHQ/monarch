import Foundation
import UniformTypeIdentifiers

/// A file chosen, dropped or picked in a ``MonarchFileUpload``.
public struct MonarchFileUploadFile: Hashable, Sendable {
    /// Where the file is. A file from the document picker is security-scoped:
    /// call `startAccessingSecurityScopedResource()` before reading it. A
    /// photo or a dropped file is a copy in the temporary directory.
    public let url: URL
    /// The file name with its extension.
    public let name: String
    /// The size in bytes, when known.
    public let size: Int64?
    /// The type, when known.
    public let contentType: UTType?

    /// A file described by the app.
    ///
    /// - Parameters:
    ///   - url: Where the file is.
    ///   - name: The file name with its extension.
    ///   - size: The size in bytes, when known.
    ///   - contentType: The type, when known.
    public init(url: URL, name: String, size: Int64? = nil, contentType: UTType? = nil) {
        self.url = url
        self.name = name
        self.size = size
        self.contentType = contentType
    }

    /// A file read from its URL: the name, size and type come from the file
    /// system, and the type falls back to the extension.
    ///
    /// - Parameter url: A file URL the process can read.
    public init(url: URL) {
        let values = try? url.resourceValues(forKeys: [.nameKey, .fileSizeKey, .contentTypeKey])
        self.init(
            url: url,
            name: values?.name ?? url.lastPathComponent,
            size: values?.fileSize.map(Int64.init),
            contentType: values?.contentType ?? UTType(filenameExtension: url.pathExtension)
        )
    }
}
