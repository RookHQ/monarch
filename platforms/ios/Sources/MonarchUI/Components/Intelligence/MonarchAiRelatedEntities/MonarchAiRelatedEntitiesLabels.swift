/// Replacements for the built-in strings of ``MonarchAiRelatedEntities``.
///
/// Every property is `nil` by default, which keeps the built-in string.
public struct MonarchAiRelatedEntitiesLabels: Sendable {
    /// The spoken name of the list or diagram. Built in: "Related entities".
    public var label: String?
    /// The name of a kind. Built in: "Person", "Organization", …; other kinds as given.
    public var kind: (@Sendable (MonarchAiRelatedEntityKind) -> String)?
    /// The spoken relevance, from a weight from 0 to 1. Built in: "Relevance 80%".
    public var weight: (@Sendable (Double) -> String)?
    /// One relation, from the other entity's name and the relation's label.
    /// Built in: "works at Acme", or "Acme" without a label.
    public var relation: (@Sendable (String, String?) -> String)?
    /// Introduces an entity's relations. Built in: "Related to".
    public var relatedTo: String?
    /// Shown when there are no entities. Built in: "No related entities".
    public var empty: String?

    /// Replacements for built-in strings. Pass only the ones to replace.
    public init(
        label: String? = nil,
        kind: (@Sendable (MonarchAiRelatedEntityKind) -> String)? = nil,
        weight: (@Sendable (Double) -> String)? = nil,
        relation: (@Sendable (String, String?) -> String)? = nil,
        relatedTo: String? = nil,
        empty: String? = nil
    ) {
        self.label = label
        self.kind = kind
        self.weight = weight
        self.relation = relation
        self.relatedTo = relatedTo
        self.empty = empty
    }
}
