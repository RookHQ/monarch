/// One snippet of ``MonarchCodeSnippetTabs``: the same example in one
/// language or tool.
public struct MonarchCodeSnippet: Identifiable, Sendable, Hashable {
    /// The value the tabs select and `syncKey` stores, such as `"python"`.
    /// Snippet sets that share a `syncKey` follow each other by this value.
    public let id: String
    /// The tab's label, such as "Python" or "cURL".
    public let label: String
    /// The code, exactly as it should be copied.
    public let code: String
    /// The language id for highlighting, such as `"python"` or `"bash"`.
    public let language: String?
    /// The code block's header, such as a file name. Defaults to the tab's label, as on the web.
    public let title: String?

    /// A snippet.
    ///
    /// - Parameters:
    ///   - label: The tab's label, already localised.
    ///   - code: The code, exactly as it should be copied.
    ///   - language: The language id for highlighting.
    ///   - id: The value the tabs select and store, such as `"python"`.
    ///     Defaults to `label`, as on the web.
    ///   - title: The code block's header, such as a file name.
    public init(_ label: String, code: String, language: String? = nil, id: String? = nil, title: String? = nil) {
        self.label = label
        self.code = code
        self.language = language
        self.id = id ?? label
        self.title = title
    }
}
