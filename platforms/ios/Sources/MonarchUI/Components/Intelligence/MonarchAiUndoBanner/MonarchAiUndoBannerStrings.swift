/// Built-in strings of the AI undo banner, from the `MonarchAiUndoBanner` strings table.
enum MonarchAiUndoBannerStrings {
    private static let table = "MonarchAiUndoBanner"

    /// Read before the title.
    static var aiPrefix: String { MonarchStrings.string("monarch.aiUndoBanner.aiPrefix", table: table) }
    /// The undo button.
    static var undo: String { MonarchStrings.string("monarch.aiUndoBanner.undo", table: table) }
    /// The audit link.
    static var audit: String { MonarchStrings.string("monarch.aiUndoBanner.audit", table: table) }
    /// The dismiss button.
    static var dismiss: String { MonarchStrings.string("monarch.aiUndoBanner.dismiss", table: table) }
    /// The disclosure title for many changes.
    static func showChanges(_ count: Int) -> String {
        MonarchStrings.string("monarch.aiUndoBanner.showChanges", table: table, count)
    }
    /// What VoiceOver reads for a change.
    static func change(_ label: String, _ before: String, _ after: String) -> String {
        MonarchStrings.string("monarch.aiUndoBanner.change", table: table, label, before, after)
    }
    /// Shown after a successful undo.
    static var undone: String { MonarchStrings.string("monarch.aiUndoBanner.undone", table: table) }
    /// Shown when the undo fails.
    static var undoFailed: String { MonarchStrings.string("monarch.aiUndoBanner.undoFailed", table: table) }
    /// Shown when the undo window closes.
    static var expired: String { MonarchStrings.string("monarch.aiUndoBanner.expired", table: table) }
}
