/// One consequence of an action, shown by ``MonarchConsequenceList``.
public struct MonarchConsequence: Identifiable, Hashable, Sendable {
    /// A stable identifier.
    public let id: String
    /// What happens to the thing.
    public let kind: MonarchConsequenceKind
    /// What happens, in plain words: "tasks will be deleted".
    public let label: String
    /// A second, quieter line with specifics.
    public let detail: String?
    /// How many things are affected. Shown before the label.
    public let count: Int?

    /// A consequence.
    ///
    /// - Parameters:
    ///   - kind: What happens to the thing.
    ///   - label: What happens, in plain words, already localised.
    ///   - detail: A second, quieter line with specifics.
    ///   - count: How many things are affected. Formatted for the locale and shown before the label.
    ///   - id: A stable identifier. The default is made of the kind and the label.
    public init(
        _ kind: MonarchConsequenceKind,
        _ label: String,
        detail: String? = nil,
        count: Int? = nil,
        id: String? = nil
    ) {
        self.id = id ?? "\(kind)-\(label)"
        self.kind = kind
        self.label = label
        self.detail = detail
        self.count = count
    }
}
