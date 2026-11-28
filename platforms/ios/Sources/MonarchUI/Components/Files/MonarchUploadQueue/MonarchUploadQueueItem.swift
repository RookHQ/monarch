/// One upload in a ``MonarchUploadQueue``, as the app reports it.
public struct MonarchUploadQueueItem: Identifiable, Sendable, Hashable {
    /// A stable, unique id.
    public let id: String
    /// The file name, shown in the row and used in spoken names.
    public let name: String
    /// The size in bytes. Shown, and used to weight the total progress.
    public let size: Int64?
    /// The MIME type; with the name it picks the type icon.
    public let contentType: String?
    /// Where the upload is.
    public let status: MonarchUploadStatus
    /// The uploaded part, 0 through 1, while uploading. `nil` is unknown.
    public let progress: Double?
    /// Why the upload failed, shown under the name.
    public let errorMessage: String?
    /// Whether retry is offered for a failed or cancelled upload.
    public let isRetryable: Bool
    /// A status line that replaces the built-in one, such as "Checking for viruses".
    public let detail: String?

    /// An upload.
    ///
    /// - Parameters:
    ///   - id: A stable, unique id.
    ///   - name: The file name.
    ///   - size: The size in bytes.
    ///   - contentType: The MIME type.
    ///   - status: Where the upload is.
    ///   - progress: The uploaded part, 0 through 1, while uploading.
    ///   - errorMessage: Why the upload failed.
    ///   - isRetryable: Whether retry is offered for a failed or cancelled upload.
    ///   - detail: A status line that replaces the built-in one.
    public init(
        id: String,
        name: String,
        size: Int64? = nil,
        contentType: String? = nil,
        status: MonarchUploadStatus,
        progress: Double? = nil,
        errorMessage: String? = nil,
        isRetryable: Bool = true,
        detail: String? = nil
    ) {
        self.id = id
        self.name = name
        self.size = size
        self.contentType = contentType
        self.status = status
        self.progress = progress
        self.errorMessage = errorMessage
        self.isRetryable = isRetryable
        self.detail = detail
    }
}
