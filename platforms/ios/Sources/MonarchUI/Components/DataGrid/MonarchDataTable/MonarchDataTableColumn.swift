import CoreGraphics

/// A column of ``MonarchDataTable``: its header, the text of each cell, and
/// how it sorts, aligns and hides.
///
/// ```swift
/// MonarchDataTableColumn<Ticket>("Subject", value: { $0.subject })
/// MonarchDataTableColumn<Ticket>(
///     "Age",
///     isNumeric: true,
///     value: { $0.ageText },
///     sortBy: { $0.age < $1.age }
/// )
/// ```
///
/// Cells are text, so the same column draws a table cell at regular width
/// and a labelled line in the compact row.
public struct MonarchDataTableColumn<Row>: Identifiable, Sendable {
    /// The column's id, as used in the sort, the hidden set and the order.
    public let id: String
    /// The header text, already localised.
    public var title: String
    /// Whether the cells hold numbers: they align to the trailing edge with tabular digits.
    public var isNumeric: Bool
    /// Whether people can sort by this column.
    public var isSortable: Bool
    /// Whether people can hide this column with ``MonarchColumnChooser``.
    public var isHideable: Bool
    /// The ideal width of the column in the table layout, or `nil` to share the width.
    public var width: CGFloat?
    /// The text of the column's cell in a row. An empty string is an empty cell.
    public let value: @Sendable (Row) -> String
    /// Whether one row sorts before another in ascending order, or `nil` to compare the cell texts.
    public let sortBy: (@Sendable (Row, Row) -> Bool)?
    /// The tone of a row's cell shown as a ``MonarchBadge`` instead of plain text, or `nil` for plain text.
    public let tone: (@Sendable (Row) -> MonarchTone?)?

    /// A column.
    ///
    /// - Parameters:
    ///   - title: The header text, already localised.
    ///   - id: The column's id. By default the title.
    ///   - isNumeric: Whether the cells hold numbers.
    ///   - isSortable: Whether people can sort by this column.
    ///   - isHideable: Whether people can hide this column.
    ///   - width: The ideal width in the table layout.
    ///   - value: The text of the cell in a row.
    ///   - sortBy: Whether one row sorts before another in ascending order. By
    ///     default the cell texts are compared as Finder does, so "Item 9"
    ///     comes before "Item 10". Pass it for numbers with grouping, dates and amounts.
    ///   - tone: The tone of the cell in a row, to show the text as a badge, as for a status. `nil`, or a `nil`
    ///     result, keeps plain text.
    public init(
        _ title: String,
        id: String? = nil,
        isNumeric: Bool = false,
        isSortable: Bool = true,
        isHideable: Bool = true,
        width: CGFloat? = nil,
        value: @escaping @Sendable (Row) -> String,
        sortBy: (@Sendable (Row, Row) -> Bool)? = nil,
        tone: (@Sendable (Row) -> MonarchTone?)? = nil
    ) {
        self.id = id ?? title
        self.title = title
        self.isNumeric = isNumeric
        self.isSortable = isSortable
        self.isHideable = isHideable
        self.width = width
        self.value = value
        self.sortBy = sortBy
        self.tone = tone
    }

    /// The column as ``MonarchColumnChooser`` lists it.
    public var chooserColumn: MonarchColumnChooserColumn {
        MonarchColumnChooserColumn(title, id: id, isHideable: isHideable)
    }
}
