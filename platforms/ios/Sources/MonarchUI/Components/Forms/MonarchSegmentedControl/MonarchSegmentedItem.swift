/// One segment of a ``MonarchSegmentedControl``.
///
/// ```swift
/// MonarchSegmentedItem("Open", value: Filter.open, count: "12")
/// MonarchSegmentedItem("List", value: View.list, systemImage: "list.bullet", isIconOnly: true)
/// ```
public struct MonarchSegmentedItem<Value: Hashable>: Identifiable {
    /// The value the selection takes when the segment is chosen.
    public let value: Value
    /// The label, already localised. It is the spoken name even when it is not shown.
    public let title: String
    /// An SF Symbol shown before the label, or `nil`.
    public let systemImage: String?
    /// A count shown after the label, or `nil`.
    public let count: String?
    /// Whether only the icon is drawn. The title is still spoken.
    public let isIconOnly: Bool
    /// Whether the segment cannot be chosen.
    public let isDisabled: Bool

    /// The identity of the segment, which is its value.
    public var id: Value { value }

    /// A segment.
    ///
    /// - Parameters:
    ///   - title: The label, already localised.
    ///   - value: The value the selection takes when the segment is chosen. Unique in the control.
    ///   - systemImage: An SF Symbol shown before the label.
    ///   - count: A count shown after the label. It is read with the label.
    ///   - isIconOnly: Draws only the icon. Needs `systemImage`; without one the title is shown.
    ///   - isDisabled: Whether the segment cannot be chosen.
    public init(
        _ title: String,
        value: Value,
        systemImage: String? = nil,
        count: String? = nil,
        isIconOnly: Bool = false,
        isDisabled: Bool = false
    ) {
        self.title = title
        self.value = value
        self.systemImage = systemImage
        self.count = count
        self.isIconOnly = isIconOnly
        self.isDisabled = isDisabled
    }
}

extension MonarchSegmentedItem: Sendable where Value: Sendable {}
