/// Who or what a memory is about. ``MonarchAiMemoryPanel`` groups memories by it.
@_spi(Experimental) public struct MonarchAiMemoryEntity: Sendable, Hashable, Codable {
    /// What the entity is.
    public var kind: MonarchAiMemoryEntityKind
    /// Its name, for example "Ada Lovelace".
    public var name: String

    /// An entity.
    ///
    /// - Parameters:
    ///   - kind: What the entity is.
    ///   - name: Its name.
    public init(kind: MonarchAiMemoryEntityKind, name: String) {
        self.kind = kind
        self.name = name
    }
}
