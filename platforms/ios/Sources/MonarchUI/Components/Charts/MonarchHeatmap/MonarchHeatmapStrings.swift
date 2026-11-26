import Foundation

/// Built-in strings of the heatmap, from the `MonarchHeatmap` strings table.
enum MonarchHeatmapStrings {
    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.heatmap.\(key)", table: "MonarchHeatmap")
    }

    /// The table heading of the row names.
    static var rowHeader: String { string("rowHeader") }
    /// Before the lightest legend step.
    static var less: String { string("less") }
    /// After the darkest legend step.
    static var more: String { string("more") }
    /// The legend step for cells without data.
    static var noData: String { string("noData") }
    /// The lightest positive range: cut point.
    static var rangeBelow: String { string("rangeBelow") }
    /// A middle range: from, to.
    static var range: String { string("range") }
    /// The darkest range: cut point.
    static var rangeAbove: String { string("rangeAbove") }
    /// The size of the matrix: rows, columns.
    static var size: String { string("size") }
    /// One row: count.
    static var rowCountOne: String { string("rowCountOne") }
    /// Several rows: count.
    static var rowCount: String { string("rowCount") }
    /// One column: count.
    static var columnCountOne: String { string("columnCountOne") }
    /// Several columns: count.
    static var columnCount: String { string("columnCount") }
    /// The highest cell: the cell as read.
    static var highest: String { string("highest") }
    /// A cell: row, column, value.
    static var cell: String { string("cell") }

    /// Fills a format's `%@` placeholders.
    static func fill(_ format: String, _ arguments: String...) -> String {
        String(format: format, arguments: arguments)
    }
}
