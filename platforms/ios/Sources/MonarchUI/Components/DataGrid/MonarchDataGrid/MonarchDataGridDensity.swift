/// The space above and below the cells of ``MonarchDataGrid``.
public enum MonarchDataGridDensity: String, Sendable, Hashable, CaseIterable {
    /// Room around each value. The default.
    case comfortable
    /// Less padding, for many rows. Cells keep a 44-point touch target.
    case compact
}
