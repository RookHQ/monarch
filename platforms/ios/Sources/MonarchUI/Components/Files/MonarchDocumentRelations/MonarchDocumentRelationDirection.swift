/// Which way a ``MonarchDocumentRelation`` reads.
@_spi(Experimental) public enum MonarchDocumentRelationDirection: Sendable, Hashable, CaseIterable {
    /// This document → the target, read as the type. The default.
    case outgoing
    /// The target → this document, read as the inverse of the type.
    case incoming
}
