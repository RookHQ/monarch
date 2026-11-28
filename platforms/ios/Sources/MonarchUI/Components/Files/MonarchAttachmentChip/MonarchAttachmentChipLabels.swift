/// Replacements for the built-in strings of ``MonarchAttachmentChip``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchAttachmentChipLabels: Sendable {
    /// The spoken name of the remove button. Receives the file name. Built
    /// in: "Remove report.pdf".
    public var remove: (@Sendable (String) -> String)?
    /// The spoken name of the retry button. Receives the file name. Built
    /// in: "Retry report.pdf".
    public var retry: (@Sendable (String) -> String)?
    /// The spoken name of the progress bar. Receives the file name. Built
    /// in: "Uploading report.pdf".
    public var uploading: (@Sendable (String) -> String)?
    /// Shown while waiting to start. Built in: "Waiting".
    public var queued: String?
    /// Shown when the upload failed and no error text is given. Built in:
    /// "Upload failed".
    public var failed: String?
    /// Shown when the upload was cancelled. Built in: "Cancelled".
    public var cancelled: String?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        remove: (@Sendable (String) -> String)? = nil,
        retry: (@Sendable (String) -> String)? = nil,
        uploading: (@Sendable (String) -> String)? = nil,
        queued: String? = nil,
        failed: String? = nil,
        cancelled: String? = nil
    ) {
        self.remove = remove
        self.retry = retry
        self.uploading = uploading
        self.queued = queued
        self.failed = failed
        self.cancelled = cancelled
    }
}
