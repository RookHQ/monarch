/// Where a pinned column of ``MonarchDataGrid`` stays while the other
/// columns scroll sideways.
public enum MonarchDataGridPin: String, Sendable, Hashable, CaseIterable {
    /// The column stays at the leading edge.
    case start
    /// The column stays at the trailing edge.
    case end
}
