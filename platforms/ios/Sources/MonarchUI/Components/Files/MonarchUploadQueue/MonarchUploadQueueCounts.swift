/// How many uploads of a ``MonarchUploadQueue`` are in each status.
public struct MonarchUploadQueueCounts: Sendable, Hashable {
    /// All uploads.
    public let total: Int
    /// Waiting to start.
    public let queued: Int
    /// Sending.
    public let uploading: Int
    /// Uploaded.
    public let done: Int
    /// Failed.
    public let failed: Int
    /// Cancelled.
    public let cancelled: Int

    /// Counts uploads by status.
    ///
    /// - Parameter items: The uploads.
    public init(_ items: [MonarchUploadQueueItem]) {
        total = items.count
        queued = items.filter { $0.status == .queued }.count
        uploading = items.filter { $0.status == .uploading }.count
        done = items.filter { $0.status == .done }.count
        failed = items.filter { $0.status == .failed }.count
        cancelled = items.filter { $0.status == .cancelled }.count
    }

    /// Uploads that can still finish.
    public var active: Int { queued + uploading }
    /// Uploads that ended: uploaded, failed or cancelled.
    public var finished: Int { done + failed + cancelled }
}
