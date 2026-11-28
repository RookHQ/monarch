/// The artifact ``MonarchAiArtifactCanvas`` shows: one version of it, with
/// its full content.
@_spi(Experimental) public struct MonarchAiCanvasArtifact: Sendable, Hashable, Identifiable {
    /// A stable id.
    public var id: String
    /// What the artifact is; picks the preview and the default code language.
    public var kind: MonarchAiArtifactKind
    /// The title.
    public var title: String
    /// The version shown, counting from 1.
    public var version: Int
    /// The content as text: Markdown for documents and tables, source for
    /// code, an HTML document for `html`, an image address for `image`.
    /// Untrusted model output: HTML only ever renders in a sandboxed web view.
    public var content: String
    /// The language of the Code tab, such as "swift". Defaults by kind.
    public var language: String?

    /// An artifact.
    ///
    /// - Parameters:
    ///   - id: A stable id.
    ///   - kind: What the artifact is.
    ///   - title: The title, already localised.
    ///   - version: The version shown, counting from 1.
    ///   - content: The content as text.
    ///   - language: The language of the Code tab.
    public init(
        id: String,
        kind: MonarchAiArtifactKind,
        title: String,
        version: Int,
        content: String,
        language: String? = nil
    ) {
        self.id = id
        self.kind = kind
        self.title = title
        self.version = version
        self.content = content
        self.language = language
    }
}
