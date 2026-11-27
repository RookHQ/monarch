/// The size of a file type icon before Dynamic Type is applied.
public enum MonarchFileTypeIconSize: Sendable, Hashable, CaseIterable {
    /// A 16-point glyph. In dense lists and inside chips.
    case sm
    /// A 20-point glyph. Beside a file name. The default.
    case md
    /// A 24-point glyph on a 40-point tinted tile. For a file card or an empty preview.
    case lg
}
