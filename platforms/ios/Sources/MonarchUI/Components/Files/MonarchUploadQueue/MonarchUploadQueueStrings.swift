/// Built-in strings of the upload queue, from the `MonarchUploadQueue` strings table.
enum MonarchUploadQueueStrings {
    private static let table = "MonarchUploadQueue"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.uploadQueue.\(key)", table: table)
    }

    private static func string(_ key: String, _ arguments: any CVarArg...) -> String {
        String(format: string(key), arguments: arguments)
    }

    /// The heading.
    static var title: String { string("title") }
    /// Waiting to start.
    static var queued: String { string("queued") }
    /// "Uploading… 40%", or "Uploading…" when the percentage is unknown.
    static func uploading(_ percent: String?) -> String {
        percent.map { string("uploadingPercent", $0) } ?? string("uploading")
    }
    /// Uploaded.
    static var done: String { string("done") }
    /// The upload failed.
    static var failed: String { string("failed") }
    /// The upload was cancelled.
    static var cancelled: String { string("cancelled") }
    /// The retry button title.
    static var retry: String { string("retry") }
    /// The spoken name of a retry button.
    static func retryLabel(_ name: String) -> String { string("retryLabel", name) }
    /// The spoken name of a cancel button.
    static func cancel(_ name: String) -> String { string("cancel", name) }
    /// The spoken name of a remove button.
    static func remove(_ name: String) -> String { string("remove", name) }
    /// The button that removes finished uploads.
    static var clearFinished: String { string("clearFinished") }
    /// The spoken name of a row's progress bar.
    static func progressLabel(_ name: String) -> String { string("progressLabel", name) }
    /// The spoken name of the total progress bar.
    static var totalLabel: String { string("totalLabel") }

    /// "2 of 5 uploaded · 1 failed · 1 cancelled".
    static func summary(_ counts: MonarchUploadQueueCounts) -> String {
        var parts = [
            MonarchStrings.plural("monarch.uploadQueue.summaryUploaded", counts.done, counts.total - counts.cancelled)
        ]
        if counts.failed > 0 { parts.append(MonarchStrings.plural("monarch.uploadQueue.summaryFailed", counts.failed)) }
        if counts.cancelled > 0 { parts.append(string("summaryCancelled", counts.cancelled)) }
        return parts.joined(separator: " · ")
    }

    /// "report.pdf uploaded."
    static func announceDone(_ name: String) -> String { string("announceDone", name) }

    /// "report.pdf failed: Too large." or "report.pdf failed."
    static func announceFailed(_ name: String, _ error: String?) -> String {
        if let error, !error.isEmpty { return string("announceFailedReason", name, error) }
        return string("announceFailed", name)
    }

    /// "All uploads finished: 4 uploaded." or, with failures, "Uploads
    /// finished: 3 uploaded, 1 failed."
    static func announceFinished(_ counts: MonarchUploadQueueCounts) -> String {
        counts.failed > 0
            ? string("announceFinishedFailed", counts.done, counts.failed)
            : string("announceFinished", counts.done)
    }
}
