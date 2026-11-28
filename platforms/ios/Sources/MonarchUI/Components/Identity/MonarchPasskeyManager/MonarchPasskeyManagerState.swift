/// Whether the passkeys of a ``MonarchPasskeyManager`` are loaded.
public enum MonarchPasskeyManagerState: Sendable, Hashable, CaseIterable {
    /// The passkeys are loaded. An empty list shows the empty state.
    case ready
    /// The passkeys are loading.
    case loading
    /// Loading failed. Pass `onRetry` to offer a retry.
    case failed
}
