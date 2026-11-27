import Foundation

/// Built-in strings of ``MonarchDiffViewer``, from the `MonarchDiffViewer` strings table.
enum MonarchDiffViewerStrings {
    private static let table = "MonarchDiffViewer"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.diffViewer." + key, table: table)
    }

    /// "Changes from Before to After".
    static func caption(_ before: String, _ after: String) -> String {
        MonarchStrings.string("monarch.diffViewer.caption", table: table, before, after)
    }
    /// The default name of the earlier side.
    static var before: String { string("before") }
    /// The default name of the later side.
    static var after: String { string("after") }
    /// The name of the layout switch.
    static var mode: String { string("mode") }
    /// The unified layout.
    static var unified: String { string("unified") }
    /// The side by side layout.
    static var split: String { string("split") }
    /// "3 added, 1 removed".
    static func stats(_ added: Int, _ removed: Int) -> String {
        MonarchStrings.string("monarch.diffViewer.stats", table: table, added, removed)
    }
    /// Read before an added line.
    static var added: String { string("added") }
    /// Read before a removed line.
    static var removed: String { string("removed") }
    /// Read for an empty line.
    static var blankLine: String { string("blankLine") }
    /// "Show 1 unchanged line" or "Show 12 unchanged lines".
    static func expand(_ count: Int) -> String {
        let key = count == 1 ? "monarch.diffViewer.expandOne" : "monarch.diffViewer.expand"
        return MonarchStrings.string(key, table: table, count)
    }
    /// The previous change button.
    static var previous: String { string("previous") }
    /// The next change button.
    static var next: String { string("next") }
    /// "Change 2 of 5".
    static func position(_ index: Int, _ total: Int) -> String {
        MonarchStrings.string("monarch.diffViewer.position", table: table, index, total)
    }
    /// Shown when the two texts are the same.
    static var noChanges: String { string("noChanges") }
}
