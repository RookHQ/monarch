import Foundation

/// Built-in strings of ``MonarchKanbanBoard``, from the `MonarchKanbanBoard` strings table.
enum MonarchKanbanBoardStrings {
    static let table = "MonarchKanbanBoard"

    /// The string for a key without placeholders, such as `moveUp`.
    static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.kanbanBoard.\(key)", table: table)
    }

    /// The string for a key with placeholders, such as "Move to Doing".
    static func string(_ key: String, _ arguments: any CVarArg...) -> String {
        String(format: string(key), arguments: arguments)
    }

    /// The `.one` or `.other` form of a key for a count (English plural rule).
    static func plural(_ key: String, _ count: Int) -> String {
        string(count == 1 ? "\(key).one" : "\(key).other", count)
    }
}
