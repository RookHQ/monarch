/// Replacements for the built-in strings of ``MonarchAiCitation``.
///
/// Every property is `nil` by default, which keeps the built-in string.
public struct MonarchAiCitationLabels: Sendable {
    /// The spoken name of the marker, given the number and the source title. Built in: "Source <n>: <title>".
    public var marker: (@Sendable (Int, String) -> String)?
    /// The heading of the card, given the number. Built in: "Source <n>".
    public var card: (@Sendable (Int) -> String)?
    /// The button that calls `onOpen`. Built in: "Open source".
    public var open: String?
    /// The link to the source's address. Built in: "Visit page".
    public var visit: String?
    /// The name of a kind of source. Return `nil` to keep the built-in name:
    /// "Web page", "Document", "Record", "File".
    public var kind: (@Sendable (MonarchAiSourceKind) -> String?)?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        marker: (@Sendable (Int, String) -> String)? = nil,
        card: (@Sendable (Int) -> String)? = nil,
        open: String? = nil,
        visit: String? = nil,
        kind: (@Sendable (MonarchAiSourceKind) -> String?)? = nil
    ) {
        self.marker = marker
        self.card = card
        self.open = open
        self.visit = visit
        self.kind = kind
    }
}
