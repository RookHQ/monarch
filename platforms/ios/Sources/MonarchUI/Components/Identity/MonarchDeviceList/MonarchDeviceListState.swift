/// Whether the devices of a ``MonarchDeviceList`` are loaded.
public enum MonarchDeviceListState: Sendable, Hashable, CaseIterable {
    /// The devices are loaded. An empty list shows the empty state.
    case ready
    /// The devices are loading.
    case loading
    /// Loading failed. Pass `onRetry` to offer a retry.
    case failed
}
