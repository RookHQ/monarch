/// Built-in strings of the AI source list, from the `MonarchAiSourceList` strings table.
///
/// ``MonarchAiSourcePreview`` reads the kind names from here too.
enum MonarchAiSourceListStrings {
    private static let table = "MonarchAiSourceList"

    /// The heading of the list.
    static var heading: String { MonarchStrings.string("monarch.aiSourceList.heading", table: table) }
    /// The heading of the used group.
    static func used(_ count: Int) -> String {
        MonarchStrings.string("monarch.aiSourceList.used", table: table, count)
    }
    /// The heading of the considered group.
    static func considered(_ count: Int) -> String {
        MonarchStrings.string("monarch.aiSourceList.considered", table: table, count)
    }
    /// The button that expands a collapsed list.
    static func showAll(_ count: Int) -> String {
        MonarchStrings.string("monarch.aiSourceList.showAll", table: table, count)
    }
    /// The button that collapses the list.
    static var showFewer: String { MonarchStrings.string("monarch.aiSourceList.showFewer", table: table) }
    /// What VoiceOver reads for a numbered source.
    static func source(_ number: Int, _ title: String) -> String {
        MonarchStrings.string("monarch.aiSourceList.source", table: table, number, title)
    }
    /// The hint of a source that opens its address.
    static var opensSource: String { MonarchStrings.string("monarch.aiSourceList.opensSource", table: table) }

    /// The name of a kind of source.
    static func kind(_ kind: MonarchAiSourceKind) -> String {
        switch kind {
        case .web: MonarchStrings.string("monarch.aiSourceList.kind.web", table: table)
        case .doc: MonarchStrings.string("monarch.aiSourceList.kind.doc", table: table)
        case .record: MonarchStrings.string("monarch.aiSourceList.kind.record", table: table)
        case .file: MonarchStrings.string("monarch.aiSourceList.kind.file", table: table)
        }
    }
}
