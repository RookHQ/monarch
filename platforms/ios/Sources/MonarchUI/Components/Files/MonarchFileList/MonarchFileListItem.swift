import SwiftUI

/// One file in a ``MonarchFileList``.
public struct MonarchFileListItem: Identifiable {
    /// A stable identifier, used for the selection.
    public let id: String
    /// The file name with its extension.
    public let name: String
    /// The size in bytes.
    public let size: Int64?
    /// The MIME type; with the name it decides the icon and the type.
    public let contentType: String?
    /// More text after the size, already localised, such as "Edited yesterday".
    public let meta: String?
    /// A preview image.
    public let thumbnail: Image?
    /// Where a preview image is, loaded with `AsyncImage`.
    public let thumbnailURL: URL?
    /// Short states shown under the name, such as "Shared".
    public let badges: [String]
    /// Whether the file cannot be opened or selected.
    public let isDisabled: Bool

    /// A file.
    ///
    /// - Parameters:
    ///   - id: A stable identifier, used for the selection.
    ///   - name: The file name with its extension.
    ///   - size: The size in bytes.
    ///   - contentType: The MIME type.
    ///   - meta: More text after the size, already localised.
    ///   - thumbnail: A preview image.
    ///   - thumbnailURL: Where a preview image is. Used when `thumbnail` is `nil`.
    ///   - badges: Short states shown under the name.
    ///   - isDisabled: Whether the file cannot be opened or selected.
    public init(
        id: String,
        name: String,
        size: Int64? = nil,
        contentType: String? = nil,
        meta: String? = nil,
        thumbnail: Image? = nil,
        thumbnailURL: URL? = nil,
        badges: [String] = [],
        isDisabled: Bool = false
    ) {
        self.id = id
        self.name = name
        self.size = size
        self.contentType = contentType
        self.meta = meta
        self.thumbnail = thumbnail
        self.thumbnailURL = thumbnailURL
        self.badges = badges
        self.isDisabled = isDisabled
    }
}
