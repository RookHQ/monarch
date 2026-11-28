/// How ``MonarchFlowLayout`` places a row in the width that is left over.
public enum MonarchFlowLayoutJustification: Sendable, Hashable, CaseIterable {
    /// Rows start at the leading edge. The layout is as wide as its widest row.
    case leading
    /// Rows are centred. The layout takes the full width it is offered.
    case center
    /// Rows end at the trailing edge. The layout takes the full width it is offered.
    case trailing
    /// The first child of a row is at the leading edge and the last at the
    /// trailing edge, with the left-over width shared between them. A row
    /// with one child starts at the leading edge.
    case spaceBetween
}
