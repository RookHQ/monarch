/// One way to install, such as an HTML tag, a framework package or a plugin, for
/// ``MonarchInstallVerify``.
@_spi(Experimental) public struct MonarchInstallSnippet: Identifiable, Hashable, Sendable {
    /// A stable, unique id.
    public let id: String
    /// The tab title, already localised, such as "HTML" or "React".
    public let label: String
    /// The code the person copies.
    public let code: String
    /// The language of the code, such as `html` or `bash`, shown on the code block.
    public let language: String?
    /// One line on where the code goes, already localised.
    public let description: String?

    /// A snippet.
    ///
    /// - Parameters:
    ///   - label: The tab title, already localised.
    ///   - code: The code the person copies.
    ///   - language: The language of the code.
    ///   - description: One line on where the code goes.
    ///   - id: A stable, unique id. By default the label.
    public init(_ label: String, code: String, language: String? = nil, description: String? = nil, id: String? = nil) {
        self.id = id ?? label
        self.label = label
        self.code = code
        self.language = language
        self.description = description
    }
}
