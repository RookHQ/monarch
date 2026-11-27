/// The colour roles of a filled, bordered surface with content on it: a
/// badge, a button, a chip, a tile.
///
/// Components resolve one of these from their state with a pure function,
/// so the rule is tested without a view, and draw it with `monarchSurface`.
struct MonarchSurfaceAppearance: Equatable {
    /// The fill, or `nil` for no fill.
    let background: MonarchColorRole?
    /// The colour of text and icons on the surface.
    let foreground: MonarchColorRole
    /// The border, or `nil` for no border.
    let border: MonarchColorRole?
}
