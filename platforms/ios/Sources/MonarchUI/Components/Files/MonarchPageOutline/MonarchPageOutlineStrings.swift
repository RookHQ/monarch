import Foundation

/// Built-in strings of the page outline, from the `MonarchPageOutline` strings table.
enum MonarchPageOutlineStrings {
    private static let table = "MonarchPageOutline"

    /// The title and the name of the outline.
    static var title: String { MonarchStrings.string("monarch.pageOutline.title", table: table) }
    /// Shown when there are no headings.
    static var empty: String { MonarchStrings.string("monarch.pageOutline.empty", table: table) }
    /// Read after the current heading.
    static var current: String { MonarchStrings.string("monarch.pageOutline.current", table: table) }

    /// The stats line, with the word count formatted for the locale.
    static func stats(_ stats: MonarchPageOutlineStats, locale: Locale) -> String {
        let key = stats.words == 1 ? "monarch.pageOutline.statsOne" : "monarch.pageOutline.stats"
        return MonarchStrings.string(
            key,
            table: table,
            stats.words.formatted(.number.locale(locale)),
            stats.minutes
        )
    }
}
