import Foundation

/// How the cells of a ``MonarchDataGrid`` column edit in place, with a
/// ``MonarchEditableCell``: the editor, the saved value of a row and how a new
/// value is saved.
///
/// ```swift
/// MonarchDataGridEdit<Invoice>(
///     isRequired: true,
///     value: { .text($0.customer) },
///     onCommit: { invoice, value in try await api.rename(invoice, to: value) }
/// )
/// ```
///
/// The value is hoisted: `onCommit` saves the new value and the app passes the
/// changed rows back to the grid. While it runs the cell shows progress; when
/// it throws, the cell keeps the editor open with the error's description.
public struct MonarchDataGridEdit<Row> {
    /// The editor a cell opens: text, number, select or date.
    public var kind: MonarchEditableCellKind
    /// The choices of a select cell.
    public var options: [MonarchEditableCellOption]
    /// The text of an empty cell and the prompt of text and number fields.
    public var placeholder: String?
    /// The lowest value of a number cell.
    public var minimum: Double?
    /// The highest value of a number cell.
    public var maximum: Double?
    /// The earliest day of a date cell.
    public var minimumDate: Date?
    /// The latest day of a date cell.
    public var maximumDate: Date?
    /// Whether an empty value is refused.
    public var isRequired: Bool
    /// Whether a row's cell can be edited. A cell that cannot is read-only text.
    public let isEditable: ((Row) -> Bool)?
    /// The text of a value in view mode, or `nil` for the cell's own formatting.
    public let format: (@MainActor (MonarchEditableCellValue) -> String)?
    /// Returns a message to refuse a row's new value before it is saved, or `nil` to allow it.
    public let validate: (@MainActor (Row, MonarchEditableCellValue) -> String?)?
    /// The saved value of a row's cell.
    public let value: (Row) -> MonarchEditableCellValue
    /// Saves a row's new value. Throw to keep the editor open with the error.
    public let onCommit: @MainActor (Row, MonarchEditableCellValue) async throws -> Void

    /// How a column edits.
    ///
    /// - Parameters:
    ///   - kind: The editor: text, number, select or date.
    ///   - options: The choices of a select cell.
    ///   - placeholder: The text of an empty cell and the prompt of the field.
    ///   - minimum: The lowest value of a number cell.
    ///   - maximum: The highest value of a number cell.
    ///   - minimumDate: The earliest day of a date cell.
    ///   - maximumDate: The latest day of a date cell.
    ///   - isRequired: Whether an empty value is refused.
    ///   - isEditable: Whether a row's cell can be edited. By default every row's.
    ///   - format: The text of a value in view mode. By default the option
    ///     label, the number or day in the locale, or the text.
    ///   - validate: Returns a message to refuse a row's new value, or `nil`.
    ///   - value: The saved value of a row's cell.
    ///   - onCommit: Saves a row's new value. Not called when it is unchanged.
    public init(
        kind: MonarchEditableCellKind = .text,
        options: [MonarchEditableCellOption] = [],
        placeholder: String? = nil,
        minimum: Double? = nil,
        maximum: Double? = nil,
        minimumDate: Date? = nil,
        maximumDate: Date? = nil,
        isRequired: Bool = false,
        isEditable: ((Row) -> Bool)? = nil,
        format: (@MainActor (MonarchEditableCellValue) -> String)? = nil,
        validate: (@MainActor (Row, MonarchEditableCellValue) -> String?)? = nil,
        value: @escaping (Row) -> MonarchEditableCellValue,
        onCommit: @escaping @MainActor (Row, MonarchEditableCellValue) async throws -> Void
    ) {
        self.kind = kind
        self.options = options
        self.placeholder = placeholder
        self.minimum = minimum
        self.maximum = maximum
        self.minimumDate = minimumDate
        self.maximumDate = maximumDate
        self.isRequired = isRequired
        self.isEditable = isEditable
        self.format = format
        self.validate = validate
        self.value = value
        self.onCommit = onCommit
    }

    /// Whether a row's cell can be edited.
    func canEdit(_ row: Row) -> Bool {
        isEditable?(row) ?? true
    }
}
