import CoreGraphics

/// A column of ``MonarchDataGrid``: its header, the text of each cell, its
/// width and pin, how it sorts and hides, and how its cells edit.
///
/// ```swift
/// MonarchDataGridColumn<Invoice>("Invoice", id: "number", isHideable: false, pinned: .start, value: { $0.number })
/// MonarchDataGridColumn<Invoice>(
///     "Customer",
///     edit: MonarchDataGridEdit(value: { .text($0.customer) }, onCommit: { invoice, value in try await save(invoice, value) }),
///     value: { $0.customer }
/// )
/// ```
///
/// The cell text is used for reading, sorting and the record list; a column
/// with `edit` shows a ``MonarchEditableCell`` instead in the grid and in the
/// record editor.
public struct MonarchDataGridColumn<Row>: Identifiable {
    /// The column's id, as used in the sort, the hidden set, the order, pins and widths.
    public let id: String
    /// The header text, already localised.
    public var title: String
    /// Whether the cells hold numbers: they align to the trailing edge with tabular digits.
    public var isNumeric: Bool
    /// Whether people can sort by this column.
    public var isSortable: Bool
    /// Whether people can hide this column.
    public var isHideable: Bool
    /// Whether people can pin and unpin this column.
    public var isPinnable: Bool
    /// The column's own pin, used while the grid is given no pinning.
    public var pinned: MonarchDataGridPin?
    /// The column's width in points, or `nil` for the default of 160 points.
    public var width: CGFloat?
    /// How the cells edit, or `nil` for read-only cells.
    public var edit: MonarchDataGridEdit<Row>?
    /// The text of the column's cell in a row. An empty string is an empty cell.
    public let value: @Sendable (Row) -> String
    /// Whether one row sorts before another in ascending order, or `nil` to compare the cell texts.
    public let sortBy: (@Sendable (Row, Row) -> Bool)?
    /// The tone of a row's read-only cell shown as a ``MonarchBadge`` instead of plain text, or `nil` for text.
    public let tone: (@Sendable (Row) -> MonarchTone?)?

    /// A column.
    ///
    /// - Parameters:
    ///   - title: The header text, already localised.
    ///   - id: The column's id. By default the title.
    ///   - isNumeric: Whether the cells hold numbers.
    ///   - isSortable: Whether people can sort by this column.
    ///   - isHideable: Whether people can hide this column.
    ///   - isPinnable: Whether people can pin and unpin this column.
    ///   - pinned: The column's own pin, used while the grid is given no pinning.
    ///   - width: The width in points. By default 160.
    ///   - edit: How the cells edit. By default they are read-only.
    ///   - value: The text of the cell in a row.
    ///   - sortBy: Whether one row sorts before another ascending. By default
    ///     the cell texts are compared as Finder does.
    ///   - tone: The tone of the read-only cell, to show its text as a badge, as for a status.
    public init(
        _ title: String,
        id: String? = nil,
        isNumeric: Bool = false,
        isSortable: Bool = true,
        isHideable: Bool = true,
        isPinnable: Bool = true,
        pinned: MonarchDataGridPin? = nil,
        width: CGFloat? = nil,
        edit: MonarchDataGridEdit<Row>? = nil,
        value: @escaping @Sendable (Row) -> String,
        sortBy: (@Sendable (Row, Row) -> Bool)? = nil,
        tone: (@Sendable (Row) -> MonarchTone?)? = nil
    ) {
        self.id = id ?? title
        self.title = title
        self.isNumeric = isNumeric
        self.isSortable = isSortable
        self.isHideable = isHideable
        self.isPinnable = isPinnable
        self.pinned = pinned
        self.width = width
        self.edit = edit
        self.value = value
        self.sortBy = sortBy
        self.tone = tone
    }

    /// The column as ``MonarchDataTable`` takes it, sharing its ordering and sorting rules.
    public var tableColumn: MonarchDataTableColumn<Row> {
        MonarchDataTableColumn(
            title,
            id: id,
            isNumeric: isNumeric,
            isSortable: isSortable,
            isHideable: isHideable,
            width: width,
            value: value,
            sortBy: sortBy,
            tone: tone
        )
    }

    /// The column as ``MonarchColumnChooser`` lists it.
    public var chooserColumn: MonarchColumnChooserColumn {
        MonarchColumnChooserColumn(title, id: id, isHideable: isHideable)
    }
}
