/// Whether the alerts of a ``MonarchSecurityAlertCard`` are loaded.
public enum MonarchSecurityAlertCardState: Sendable, Hashable, CaseIterable {
    /// The alerts are loaded. An empty list shows the empty state.
    case ready
    /// The alerts are loading.
    case loading
    /// Loading failed. Pass `onRetry` to offer a retry.
    case failed
}
