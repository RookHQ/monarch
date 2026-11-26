/// How a provenance note is drawn.
public enum MonarchProvenanceVariant: Sendable, Hashable, CaseIterable {
    /// A small bordered tag next to a value. The default.
    case chip
    /// Plain muted text, for use inside a sentence or a dense row.
    case inline
}
