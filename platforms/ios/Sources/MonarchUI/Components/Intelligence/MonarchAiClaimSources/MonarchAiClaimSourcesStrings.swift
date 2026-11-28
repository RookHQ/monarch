/// Built-in strings of the claim sources view, from the `MonarchAiClaimSources` strings table.
enum MonarchAiClaimSourcesStrings {
    private static let table = "MonarchAiClaimSources"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.aiClaimSources.\(key)", table: table)
    }

    private static func string(_ key: String, _ argument: String) -> String {
        MonarchStrings.string("monarch.aiClaimSources.\(key)", table: table, argument)
    }

    private static func string(_ key: String, count: Int) -> String {
        MonarchStrings.string("monarch.aiClaimSources.\(key).\(count == 1 ? "one" : "other")", table: table, count)
    }

    /// The heading.
    static var heading: String { string("heading") }
    /// The summary line.
    static func summary(_ counts: MonarchAiClaimCounts) -> String {
        guard counts.total > 0 else { return string("summary.none") }
        let parts = [
            (counts.supported, "summary.supported"),
            (counts.partial, "summary.partial"),
            (counts.unsupported, "summary.unsupported"),
            (counts.conflicting, "summary.conflicting"),
            (counts.unchecked, "summary.unchecked"),
        ]
        .filter { $0.0 > 0 }
        .map { MonarchStrings.string("monarch.aiClaimSources.\($0.1)", table: table, $0.0) }
        return MonarchStrings.string(
            "monarch.aiClaimSources.summary.\(counts.total == 1 ? "one" : "other")",
            table: table,
            counts.total,
            parts.joined(separator: string("summary.separator"))
        )
    }
    /// The name of the filter.
    static var filterLabel: String { string("filterLabel") }
    /// Every claim.
    static var all: String { string("all") }
    /// Claims that are not fully supported.
    static var needsChecking: String { string("needsChecking") }
    /// The support level in words.
    static func support(_ support: MonarchAiClaimSupport) -> String { string("support.\(key(support))") }
    /// The note of a claim that is not fully supported. The supported level has none.
    static func note(_ support: MonarchAiClaimSupport) -> String {
        support == .supported ? "" : string("note.\(key(support))")
    }
    /// The number of passages.
    static func passages(_ count: Int) -> String {
        count == 0 ? string("passages.none") : string("passages", count: count)
    }
    /// The spoken name of a claim's passages.
    static func evidenceLabel(_ claim: String) -> String { string("evidenceLabel", claim) }
    /// The stance of a passage.
    static func stance(_ stance: MonarchAiClaimEvidenceStance) -> String {
        string(stance == .supports ? "stance.supports" : "stance.contradicts")
    }
    /// A source that is not in the list.
    static var unknownSource: String { string("unknownSource") }
    /// The publication date.
    static func published(_ date: String) -> String { string("published", date) }
    /// Opens a source.
    static var openSource: String { string("openSource") }
    /// Tells the actions of passages apart.
    static func fromSource(_ title: String) -> String { string("fromSource", title) }
    /// Copies a passage.
    static var copyQuote: String { string("copyQuote") }
    /// After copying.
    static var copied: String { string("copied") }
    /// Announced after copying.
    static var copiedAnnouncement: String { string("copiedAnnouncement") }
    /// Reports a problem.
    static var flag: String { string("flag") }
    /// Checks an unchecked claim.
    static var verify: String { string("verify") }
    /// No claims.
    static var empty: String { string("empty") }
    /// Nothing needs checking.
    static var noneNeedChecking: String { string("noneNeedChecking") }
    /// No passages and no note.
    static var noEvidence: String { string("noEvidence") }
    /// Announced when the filter changes.
    static func showing(_ shown: Int, _ total: Int) -> String {
        MonarchStrings.string("monarch.aiClaimSources.showing", table: table, shown, total)
    }

    private static func key(_ support: MonarchAiClaimSupport) -> String {
        switch support {
        case .supported: "supported"
        case .partial: "partial"
        case .unsupported: "unsupported"
        case .conflicting: "conflicting"
        case .unchecked: "unchecked"
        }
    }
}
