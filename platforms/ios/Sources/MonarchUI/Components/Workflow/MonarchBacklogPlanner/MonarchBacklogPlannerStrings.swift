import Foundation

/// Built-in strings of ``MonarchBacklogPlanner``, from the `MonarchBacklogPlanner` strings table.
enum MonarchBacklogPlannerStrings {
    private static let table = "MonarchBacklogPlanner"

    /// The string for a key without placeholders, such as `moveTo`.
    static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.backlogPlanner.\(key)", table: table)
    }

    /// The string for a key with placeholders, such as "3 items" or "Move PROJ-1 to…".
    static func string(_ key: String, _ arguments: any CVarArg...) -> String {
        String(format: string(key), arguments: arguments)
    }

    /// The `.one` or `.other` form of a key for a count (English plural rule).
    static func plural(_ key: String, _ count: Int) -> String {
        string(count == 1 ? "\(key).one" : "\(key).other", count)
    }
}
