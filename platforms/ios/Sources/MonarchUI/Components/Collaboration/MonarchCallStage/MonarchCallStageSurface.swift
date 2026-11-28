/// Where a ``MonarchCallStage`` is shown.
@_spi(Experimental) public enum MonarchCallStageSurface: Sendable, Hashable, CaseIterable {
    /// The call fills its container. The default.
    case stage
    /// A compact panel in a corner while people keep working: at most two
    /// grid columns and no tile buttons.
    case docked
    /// The content of a picture in picture window: compact, as docked, and
    /// without the picture in picture button.
    case pip
}
