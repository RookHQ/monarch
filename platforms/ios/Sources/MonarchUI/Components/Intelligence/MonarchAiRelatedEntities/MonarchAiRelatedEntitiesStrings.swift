/// Built-in strings of the related entities, from the `MonarchAiRelatedEntities` strings table.
enum MonarchAiRelatedEntitiesStrings {
    private static let table = "MonarchAiRelatedEntities"
    private static let knownKinds: Set<MonarchAiRelatedEntityKind> = [
        .person, .org, .project, .document, .record, .place, .other,
    ]

    /// The spoken name of the list or diagram.
    static var label: String { MonarchStrings.string("monarch.aiRelatedEntities.label", table: table) }

    /// The name of a kind; an unknown kind as given.
    static func kind(_ kind: MonarchAiRelatedEntityKind) -> String {
        guard knownKinds.contains(kind) else { return kind.rawValue }
        return MonarchStrings.string("monarch.aiRelatedEntities.kind.\(kind.rawValue)", table: table)
    }

    /// The spoken relevance, from an already formatted percentage.
    static func weight(_ percent: String) -> String {
        MonarchStrings.string("monarch.aiRelatedEntities.weight", table: table, percent)
    }

    /// One relation with a label.
    static func relation(_ other: String, label: String) -> String {
        MonarchStrings.string("monarch.aiRelatedEntities.relation", table: table, label, other)
    }

    /// Introduces an entity's relations.
    static var relatedTo: String { MonarchStrings.string("monarch.aiRelatedEntities.relatedTo", table: table) }

    /// Shown when there are no entities.
    static var empty: String { MonarchStrings.string("monarch.aiRelatedEntities.empty", table: table) }
}
