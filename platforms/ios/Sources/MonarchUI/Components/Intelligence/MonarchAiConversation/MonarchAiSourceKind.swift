/// What kind of thing a source is.
public enum MonarchAiSourceKind: String, Sendable, Hashable, Codable, CaseIterable {
    /// A web page.
    case web
    /// A document.
    case doc
    /// A record in a product.
    case record
    /// A file.
    case file
}
