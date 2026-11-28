/// Where a ``MonarchAiBackgroundTask`` is.
@_spi(Experimental) public enum MonarchAiBackgroundTaskStatus: String, Sendable, Hashable, CaseIterable {
    /// Waiting to start.
    case queued
    /// The agent is working.
    case running
    /// The agent waits on the person.
    case needsInput
    /// Finished with a result.
    case succeeded
    /// Finished with an error.
    case failed
    /// Stopped before it finished.
    case cancelled

    /// Whether the task has ended: succeeded, failed or cancelled.
    public var isFinished: Bool {
        self == .succeeded || self == .failed || self == .cancelled
    }
}
