/// Replacements for the built-in strings of ``MonarchAiSourceList``.
///
/// Every property is `nil` by default, which keeps the built-in string.
public struct MonarchAiSourceListLabels: Sendable {
    /// The heading of the list. Built in: "Sources".
    public var heading: String?
    /// The heading of the used group, from its count. Built in: "Used (3)".
    public var used: (@Sendable (Int) -> String)?
    /// The heading of the considered group, from its count. Built in: "Considered (2)".
    public var considered: (@Sendable (Int) -> String)?
    /// The button that expands a collapsed list, from the number of sources. Built in: "Show all 5 sources".
    public var showAll: (@Sendable (Int) -> String)?
    /// The button that collapses the list again. Built in: "Show fewer".
    public var showFewer: String?
    /// What VoiceOver reads for a numbered source, from its number and title. Built in: "Source 1: Title".
    public var source: (@Sendable (Int, String) -> String)?
    /// The hint of a source that opens its address. Built in: "Opens the source."
    public var opensSource: String?
    /// The name of a kind of source in the meta line. Built in: "Web page", "Document", "Record", "File".
    public var kind: (@Sendable (MonarchAiSourceKind) -> String)?

    /// Replacements for built-in strings. Pass only the ones to replace.
    public init(
        heading: String? = nil,
        used: (@Sendable (Int) -> String)? = nil,
        considered: (@Sendable (Int) -> String)? = nil,
        showAll: (@Sendable (Int) -> String)? = nil,
        showFewer: String? = nil,
        source: (@Sendable (Int, String) -> String)? = nil,
        opensSource: String? = nil,
        kind: (@Sendable (MonarchAiSourceKind) -> String)? = nil
    ) {
        self.heading = heading
        self.used = used
        self.considered = considered
        self.showAll = showAll
        self.showFewer = showFewer
        self.source = source
        self.opensSource = opensSource
        self.kind = kind
    }
}
