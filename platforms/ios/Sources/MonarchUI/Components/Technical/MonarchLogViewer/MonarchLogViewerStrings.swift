import Foundation

/// Built-in strings of the log viewer, from the `MonarchLogViewer` strings table.
enum MonarchLogViewerStrings {
    private static let table = "MonarchLogViewer"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.logViewer.\(key)", table: table)
    }

    /// The spoken name of the scrolling log.
    static var log: String { string("log") }
    /// The spoken name of the level filters.
    static var levels: String { string("levels") }
    /// The spoken name of the search field.
    static var search: String { string("search") }
    /// The placeholder of the search field.
    static var searchPrompt: String { string("searchPrompt") }
    /// The button that empties the search field.
    static var clearSearch: String { string("clearSearch") }
    /// The follow toggle.
    static var follow: String { string("follow") }
    /// The wrap toggle.
    static var wrap: String { string("wrap") }
    /// The jump button.
    static var jumpToLatest: String { string("jumpToLatest") }
    /// Announced when following stops.
    static var followPaused: String { string("followPaused") }
    /// Announced when following starts again.
    static var followResumed: String { string("followResumed") }
    /// Shown when there are no lines.
    static var empty: String { string("empty") }
    /// Shown when the filters hide every line.
    static var noMatches: String { string("noMatches") }
    /// Shown while the first lines load.
    static var loading: String { string("loading") }

    /// The short name of a level.
    static func levelName(_ level: MonarchLogLevel) -> String {
        switch level {
        case .debug: string("level.debug")
        case .info: string("level.info")
        case .warn: string("level.warn")
        case .error: string("level.error")
        }
    }

    /// "120 of 5,000 lines", or "5,000 lines" (or "1 line") when nothing is hidden.
    static func count(shown: Int, total: Int) -> String {
        if shown == total {
            let key = total == 1 ? "count.one" : "count.other"
            return MonarchStrings.string("monarch.logViewer.\(key)", table: table, total.formatted())
        }
        return MonarchStrings.string(
            "monarch.logViewer.count.filtered",
            table: table,
            shown.formatted(),
            total.formatted()
        )
    }

    /// "1 new line" or "3 new lines".
    static func newLines(_ count: Int) -> String {
        let key = count == 1 ? "newLines.one" : "newLines.other"
        return MonarchStrings.string("monarch.logViewer.\(key)", table: table, count.formatted())
    }
}
