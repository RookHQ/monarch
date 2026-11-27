/// Replacements for the built-in strings of ``MonarchUploadQueue``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchUploadQueueLabels: Sendable {
    /// The heading. Built in: "Uploads".
    public var title: String?
    /// Built in: "Waiting".
    public var queued: String?
    /// Receives the percentage, or `nil` when unknown. Built in: "Uploading… 40%".
    public var uploading: (@Sendable (String?) -> String)?
    /// Built in: "Uploaded".
    public var done: String?
    /// Shown when no error text is given. Built in: "Upload failed".
    public var failed: String?
    /// Built in: "Cancelled".
    public var cancelled: String?
    /// The retry button title. Built in: "Retry".
    public var retry: String?
    /// Receives the file name. Built in: "Retry upload of report.pdf".
    public var retryLabel: (@Sendable (String) -> String)?
    /// Receives the file name. Built in: "Cancel upload of report.pdf".
    public var cancel: (@Sendable (String) -> String)?
    /// Receives the file name. Built in: "Remove report.pdf from the list".
    public var remove: (@Sendable (String) -> String)?
    /// The button that removes finished uploads. Built in: "Clear finished".
    public var clearFinished: String?
    /// Receives the file name. Built in: "Upload progress of report.pdf".
    public var progressLabel: (@Sendable (String) -> String)?
    /// The spoken name of the total progress bar. Built in: "Total upload progress".
    public var totalLabel: String?
    /// The line under the heading. Built in: "2 of 5 uploaded · 1 failed".
    public var summary: (@Sendable (MonarchUploadQueueCounts) -> String)?
    /// Announced when an upload finishes. Built in: "report.pdf uploaded."
    public var announceDone: (@Sendable (String) -> String)?
    /// Announced when an upload fails. Receives the name and the error.
    /// Built in: "report.pdf failed: Too large."
    public var announceFailed: (@Sendable (String, String?) -> String)?
    /// Announced when the last active upload of several ends. Built in:
    /// "All uploads finished: 4 uploaded."
    public var announceFinished: (@Sendable (MonarchUploadQueueCounts) -> String)?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        title: String? = nil,
        queued: String? = nil,
        uploading: (@Sendable (String?) -> String)? = nil,
        done: String? = nil,
        failed: String? = nil,
        cancelled: String? = nil,
        retry: String? = nil,
        retryLabel: (@Sendable (String) -> String)? = nil,
        cancel: (@Sendable (String) -> String)? = nil,
        remove: (@Sendable (String) -> String)? = nil,
        clearFinished: String? = nil,
        progressLabel: (@Sendable (String) -> String)? = nil,
        totalLabel: String? = nil,
        summary: (@Sendable (MonarchUploadQueueCounts) -> String)? = nil,
        announceDone: (@Sendable (String) -> String)? = nil,
        announceFailed: (@Sendable (String, String?) -> String)? = nil,
        announceFinished: (@Sendable (MonarchUploadQueueCounts) -> String)? = nil
    ) {
        self.title = title
        self.queued = queued
        self.uploading = uploading
        self.done = done
        self.failed = failed
        self.cancelled = cancelled
        self.retry = retry
        self.retryLabel = retryLabel
        self.cancel = cancel
        self.remove = remove
        self.clearFinished = clearFinished
        self.progressLabel = progressLabel
        self.totalLabel = totalLabel
        self.summary = summary
        self.announceDone = announceDone
        self.announceFailed = announceFailed
        self.announceFinished = announceFinished
    }
}
