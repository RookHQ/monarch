/// Where one agent in a multi-agent run is.
@_spi(Experimental) public enum MonarchAiSubagentStatus: String, Sendable, Hashable, Codable, CaseIterable {
    /// Working on its task.
    case working
    /// Stopped to ask a person something.
    case needsInput = "needs-input"
    /// Not started yet.
    case waiting
    /// Finished.
    case done
    /// Ended with an error.
    case failed
    /// Stopped before it finished.
    case stopped
}
