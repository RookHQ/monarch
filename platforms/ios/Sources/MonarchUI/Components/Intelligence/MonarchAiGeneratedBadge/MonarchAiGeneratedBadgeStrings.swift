/// Built-in strings of the AI provenance badge, from the `MonarchAiGeneratedBadge` strings table.
enum MonarchAiGeneratedBadgeStrings {
    private static let table = "MonarchAiGeneratedBadge"

    /// The label of a kind.
    static func label(_ kind: MonarchAiGeneratedBadgeKind) -> String {
        switch kind {
        case .generated: MonarchStrings.string("monarch.aiGeneratedBadge.generated", table: table)
        case .assisted: MonarchStrings.string("monarch.aiGeneratedBadge.assisted", table: table)
        case .edited: MonarchStrings.string("monarch.aiGeneratedBadge.edited", table: table)
        }
    }

    /// The provenance line for a model alone.
    static func byModel(_ model: String) -> String {
        MonarchStrings.string("monarch.aiGeneratedBadge.byModel", table: table, model)
    }

    /// The provenance line for a model and a time.
    static func byModel(_ model: String, at time: String) -> String {
        MonarchStrings.string("monarch.aiGeneratedBadge.byModelAt", table: table, model, time)
    }
}
