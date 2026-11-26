/// Where an inline completion is.
public enum MonarchAiCompletionStatus: String, Sendable, Hashable, CaseIterable {
    /// Nothing asked and nothing shown.
    case idle
    /// The person is still typing; the request has not been sent.
    case waiting
    /// The request is in flight.
    case loading
    /// A suggestion is showing.
    case ready
    /// The last request failed.
    case error
}
