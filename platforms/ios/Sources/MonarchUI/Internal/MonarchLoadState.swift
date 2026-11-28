/// Where data a component shows has got to. The shared state of lists,
/// panels and pickers that load their content: they take this rather than
/// declaring their own ready/loading/failed enum.
enum MonarchLoadState: Equatable, Sendable {
    /// The data is here.
    case ready
    /// The data is loading; show a placeholder.
    case loading
    /// Loading failed; show the message, with a retry where there is one.
    case failed(message: String?)

    /// Whether data is loading.
    var isLoading: Bool { self == .loading }

    /// The failure message, or `nil` when loading has not failed. An empty
    /// message counts as none, so the component's built-in text is used.
    var failureMessage: String? {
        guard case .failed(let message) = self, let message, !message.isEmpty else { return nil }
        return message
    }

    /// Whether loading failed.
    var isFailed: Bool {
        if case .failed = self { return true }
        return false
    }
}
