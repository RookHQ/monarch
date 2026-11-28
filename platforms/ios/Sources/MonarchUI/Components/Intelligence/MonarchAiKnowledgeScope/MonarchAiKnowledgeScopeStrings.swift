/// Built-in strings of the knowledge scope, from the `MonarchAiKnowledgeScope` strings table.
enum MonarchAiKnowledgeScopeStrings {
    private static let table = "MonarchAiKnowledgeScope"

    /// The spoken name of the tree.
    static var label: String { MonarchStrings.string("monarch.aiKnowledgeScope.label", table: table) }

    /// The name of a kind.
    static func kind(_ kind: MonarchAiKnowledgeSourceKind) -> String {
        switch kind {
        case .space: MonarchStrings.string("monarch.aiKnowledgeScope.kind.space", table: table)
        case .document: MonarchStrings.string("monarch.aiKnowledgeScope.kind.document", table: table)
        case .app: MonarchStrings.string("monarch.aiKnowledgeScope.kind.app", table: table)
        }
    }

    /// The spoken name of a source.
    static func itemLabel(_ label: String, kind: String) -> String {
        MonarchStrings.string("monarch.aiKnowledgeScope.itemLabel", table: table, label, kind)
    }

    /// The summary under the tree.
    static func summary(_ count: Int) -> String {
        switch count {
        case 0: MonarchStrings.string("monarch.aiKnowledgeScope.summary.none", table: table)
        case 1: MonarchStrings.string("monarch.aiKnowledgeScope.summary.one", table: table)
        default: MonarchStrings.string("monarch.aiKnowledgeScope.summary", table: table, count)
        }
    }

    /// Shown when there are no sources.
    static var empty: String { MonarchStrings.string("monarch.aiKnowledgeScope.empty", table: table) }

    /// The spoken name of the show-contents button.
    static func expand(_ label: String) -> String {
        MonarchStrings.string("monarch.aiKnowledgeScope.expand", table: table, label)
    }

    /// The spoken name of the hide-contents button.
    static func collapse(_ label: String) -> String {
        MonarchStrings.string("monarch.aiKnowledgeScope.collapse", table: table, label)
    }
}
