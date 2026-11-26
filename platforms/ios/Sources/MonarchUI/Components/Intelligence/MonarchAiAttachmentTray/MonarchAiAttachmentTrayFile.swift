import Foundation

/// One file in a ``MonarchAiAttachmentTray``.
public struct MonarchAiAttachmentTrayFile: Identifiable, Sendable, Hashable {
    /// A stable id, passed to the tray's `onRemove` and `onRetry`.
    public var id: String
    /// The file name.
    public var name: String
    /// The size in bytes.
    public var size: Int64?
    /// The MIME type, such as `image/png` or `application/pdf`.
    public var contentType: String?
    /// The uploaded part, 0 through 1. `nil` or 1 when the file is ready.
    public var progress: Double?
    /// Whether the upload failed.
    public var isFailed: Bool
    /// Why the upload failed, already localised. Setting it also marks the file failed.
    public var errorMessage: String?

    /// A file in the tray.
    ///
    /// - Parameters:
    ///   - id: A stable id.
    ///   - name: The file name.
    ///   - size: The size in bytes.
    ///   - contentType: The MIME type.
    ///   - progress: The uploaded part, 0 through 1, while uploading.
    ///   - isFailed: Whether the upload failed.
    ///   - errorMessage: Why the upload failed. Setting it also marks the file failed.
    public init(
        id: String,
        name: String,
        size: Int64? = nil,
        contentType: String? = nil,
        progress: Double? = nil,
        isFailed: Bool = false,
        errorMessage: String? = nil
    ) {
        self.id = id
        self.name = name
        self.size = size
        self.contentType = contentType
        self.progress = progress
        self.isFailed = isFailed
        self.errorMessage = errorMessage
    }

    /// A file from a conversation's file part, ready unless a progress is given.
    ///
    /// - Parameters:
    ///   - part: The file part of a message.
    ///   - id: A stable id. Defaults to the part's `fileId`, else its name.
    ///   - progress: The uploaded part, 0 through 1, while uploading.
    public init(_ part: MonarchAiFilePart, id: String? = nil, progress: Double? = nil) {
        self.init(
            id: id ?? part.fileId ?? part.name,
            name: part.name,
            size: part.size.map { Int64($0) },
            contentType: part.mediaType,
            progress: progress
        )
    }
}
