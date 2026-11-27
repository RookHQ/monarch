/// What a row of a ``MonarchLinkedDocsPanel`` points at.
@_spi(Experimental) public enum MonarchLinkedDocsPanelKind: Sendable, Hashable, CaseIterable {
    /// A document in the app's own docs.
    case doc
    /// An external web page.
    case link
}
