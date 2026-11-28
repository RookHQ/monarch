/// Something the reply made that stands on its own: a document, code, a table.
public struct MonarchAiArtifactPart: Identifiable, Sendable, Hashable, Codable {
    /// The id of the artifact.
    public var id: String
    /// What the artifact is.
    public var kind: MonarchAiArtifactKind
    /// The title.
    public var title: String
    /// The version, counted from 1.
    public var version: Int

    /// An artifact.
    ///
    /// - Parameters:
    ///   - id: The id of the artifact.
    ///   - kind: What the artifact is.
    ///   - title: The title.
    ///   - version: The version.
    public init(id: String, kind: MonarchAiArtifactKind, title: String, version: Int) {
        self.id = id
        self.kind = kind
        self.title = title
        self.version = version
    }
}
