/// The layout of an event chip.
public enum MonarchEventChipVariant: Sendable, Hashable, CaseIterable {
    /// One compact line with a colour dot, for a month cell or a list in a
    /// popover. The default.
    case dot
    /// A tinted row with a colour bar and room for a detail line, for agenda
    /// rows and all-day rows.
    case bar
    /// A tinted block that fills the frame it is given, for an event placed
    /// in a time grid.
    case block
}
