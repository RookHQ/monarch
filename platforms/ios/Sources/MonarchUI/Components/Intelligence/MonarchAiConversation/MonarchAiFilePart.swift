/// A file attached to a message.
public struct MonarchAiFilePart: Sendable, Hashable, Codable {
    /// The file name.
    public var name: String
    /// The media type, for example `application/pdf`.
    public var mediaType: String
    /// Where the file lives: an upload URL, or a `data:` URL for an inlined file.
    public var url: String?
    /// The size in bytes.
    public var size: Int?
    /// The id from your upload service, sent to the model service instead of the bytes.
    public var fileId: String?

    /// A file.
    ///
    /// - Parameters:
    ///   - name: The file name.
    ///   - mediaType: The media type.
    ///   - url: Where the file lives.
    ///   - size: The size in bytes.
    ///   - fileId: The id from your upload service.
    public init(name: String, mediaType: String, url: String? = nil, size: Int? = nil, fileId: String? = nil) {
        self.name = name
        self.mediaType = mediaType
        self.url = url
        self.size = size
        self.fileId = fileId
    }
}
