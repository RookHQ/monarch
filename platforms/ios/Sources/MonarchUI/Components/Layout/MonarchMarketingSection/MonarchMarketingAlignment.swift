/// How the text of a marketing block lines up: along the leading edge, or centred.
public enum MonarchMarketingAlignment: Sendable, Hashable, CaseIterable {
    /// Text starts at the leading edge. The default for sections.
    case leading
    /// Text is centred, for a hero or a pricing band.
    case center
}
