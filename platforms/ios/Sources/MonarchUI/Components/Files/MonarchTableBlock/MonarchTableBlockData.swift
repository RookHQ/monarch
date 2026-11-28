/// The content of a ``MonarchTableBlock``: a rectangle of text cells and
/// whether the first row is a header.
///
/// The table always has at least one row and one column, every row has the
/// same number of cells, and it never grows past ``maximumRowCount`` rows or
/// ``maximumColumnCount`` columns. The same limits apply on the web.
public struct MonarchTableBlockData: Sendable, Hashable {
    /// The most rows a table can have.
    public static let maximumRowCount = 50
    /// The most columns a table can have.
    public static let maximumColumnCount = 20

    /// The cells, row by row. Every row has ``columnCount`` cells.
    public private(set) var rows: [[String]]
    /// Whether the first row is drawn and read as column headers.
    public var hasHeaderRow: Bool

    /// A table with the given cells.
    ///
    /// Short rows are padded with empty cells. Rows and columns past the
    /// limits are dropped, and an empty input gives one empty cell.
    ///
    /// - Parameters:
    ///   - rows: The cells, row by row.
    ///   - hasHeaderRow: Whether the first row holds column headers.
    public init(rows: [[String]], hasHeaderRow: Bool = true) {
        let kept = rows.prefix(Self.maximumRowCount)
        let width = min(Self.maximumColumnCount, max(1, kept.map(\.count).max() ?? 1))
        var normalized = kept.map { row in
            Array(row.prefix(width)) + Array(repeating: "", count: max(0, width - row.count))
        }
        if normalized.isEmpty { normalized = [Array(repeating: "", count: width)] }
        self.rows = normalized
        self.hasHeaderRow = hasHeaderRow
    }

    /// An empty table. The counts are clamped to 1 and the limits.
    ///
    /// - Parameters:
    ///   - rowCount: The number of rows, 3 by default.
    ///   - columnCount: The number of columns, 3 by default.
    ///   - hasHeaderRow: Whether the first row holds column headers.
    public init(rowCount: Int = 3, columnCount: Int = 3, hasHeaderRow: Bool = true) {
        let height = min(Self.maximumRowCount, max(1, rowCount))
        let width = min(Self.maximumColumnCount, max(1, columnCount))
        self.rows = Array(repeating: Array(repeating: "", count: width), count: height)
        self.hasHeaderRow = hasHeaderRow
    }

    /// The number of rows, header included.
    public var rowCount: Int { rows.count }
    /// The number of columns.
    public var columnCount: Int { rows.first?.count ?? 0 }
    /// Whether another row fits.
    public var canAddRow: Bool { rowCount < Self.maximumRowCount }
    /// Whether another column fits.
    public var canAddColumn: Bool { columnCount < Self.maximumColumnCount }
    /// Whether a row can be deleted: the last row cannot.
    public var canDeleteRow: Bool { rowCount > 1 }
    /// Whether a column can be deleted: the last column cannot.
    public var canDeleteColumn: Bool { columnCount > 1 }

    /// The text of a cell. Reading outside the table gives an empty string
    /// and writing outside it does nothing.
    public subscript(row: Int, column: Int) -> String {
        get {
            guard contains(row: row, column: column) else { return "" }
            return rows[row][column]
        }
        set {
            guard contains(row: row, column: column) else { return }
            rows[row][column] = newValue
        }
    }

    /// Whether a row is the header row.
    public func isHeader(row: Int) -> Bool {
        hasHeaderRow && row == 0
    }

    /// Inserts an empty row below `row`.
    ///
    /// - Returns: `false`, with no change, when the table is full or `row` is outside it.
    @discardableResult
    public mutating func addRow(after row: Int) -> Bool {
        guard canAddRow, rows.indices.contains(row) else { return false }
        rows.insert(Array(repeating: "", count: columnCount), at: row + 1)
        return true
    }

    /// Inserts an empty column after `column`.
    ///
    /// - Returns: `false`, with no change, when the table is full or `column` is outside it.
    @discardableResult
    public mutating func addColumn(after column: Int) -> Bool {
        guard canAddColumn, (0..<columnCount).contains(column) else { return false }
        for index in rows.indices { rows[index].insert("", at: column + 1) }
        return true
    }

    /// Deletes a row.
    ///
    /// - Returns: `false`, with no change, for the last remaining row or a row outside the table.
    @discardableResult
    public mutating func deleteRow(at row: Int) -> Bool {
        guard canDeleteRow, rows.indices.contains(row) else { return false }
        rows.remove(at: row)
        return true
    }

    /// Deletes a column.
    ///
    /// - Returns: `false`, with no change, for the last remaining column or a column outside the table.
    @discardableResult
    public mutating func deleteColumn(at column: Int) -> Bool {
        guard canDeleteColumn, (0..<columnCount).contains(column) else { return false }
        for index in rows.indices { rows[index].remove(at: column) }
        return true
    }

    private func contains(row: Int, column: Int) -> Bool {
        rows.indices.contains(row) && (0..<columnCount).contains(column)
    }
}
