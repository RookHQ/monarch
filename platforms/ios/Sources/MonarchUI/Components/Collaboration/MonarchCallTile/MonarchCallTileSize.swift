/// The size of a ``MonarchCallTile``.
public enum MonarchCallTileSize: Sendable, Hashable, CaseIterable {
    /// The filmstrip size: a smaller avatar and name, and no pin or full-screen buttons.
    case sm
    /// A tile in a grid. The default.
    case md
    /// The main speaker or the pinned tile.
    case lg
}
