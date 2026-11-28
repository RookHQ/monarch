/// Where a long-running assistant task is.
public enum MonarchAiTaskStatus: String, Sendable, Hashable, Codable, CaseIterable {
    /// Working.
    case running
    /// Stopped for now; it can resume.
    case paused
    /// Finished.
    case done
    /// Stopped with an error.
    case failed
    /// Stopped by the person.
    case cancelled
}
