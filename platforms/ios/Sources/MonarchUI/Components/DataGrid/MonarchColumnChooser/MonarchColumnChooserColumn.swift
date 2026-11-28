/// A column as ``MonarchColumnChooser`` lists it: its id, its name and
/// whether it can be hidden.
///
/// ``MonarchDataTableColumn`` gives one with `chooserColumn`.
public struct MonarchColumnChooserColumn: Identifiable, Sendable, Hashable {
    /// The column's id, as stored in the hidden set and the order.
    public var id: String
    /// The column's name, already localised.
    public var label: String
    /// Whether the column can be hidden. A column that cannot is always shown.
    public var isHideable: Bool

    /// A column.
    ///
    /// - Parameters:
    ///   - label: The column's name, already localised.
    ///   - id: The column's id. By default the label.
    ///   - isHideable: Whether the column can be hidden.
    public init(_ label: String, id: String? = nil, isHideable: Bool = true) {
        self.id = id ?? label
        self.label = label
        self.isHideable = isHideable
    }
}
