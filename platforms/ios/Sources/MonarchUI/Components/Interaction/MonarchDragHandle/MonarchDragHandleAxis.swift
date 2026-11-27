/// Which way an item moves when it is dragged by a ``MonarchDragHandle``.
public enum MonarchDragHandleAxis: Sendable, Hashable, CaseIterable {
    /// Up and down, as in a list.
    case vertical
    /// Backward and forward along a row.
    case horizontal
    /// In both directions, as in a grid.
    case both
}
