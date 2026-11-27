/// One choice of a ``MonarchSelectField``.
///
/// ```swift
/// MonarchSelectOption("In progress", value: "progress")
/// ```
public struct MonarchSelectOption<Value: Hashable>: Identifiable {
    /// The value the selection takes when the option is chosen.
    public let value: Value
    /// The label, already localised. It is also the text the field shows.
    public let title: String
    /// An SF Symbol shown before the label in the menu, or `nil`.
    public let systemImage: String?
    /// Whether the option cannot be chosen.
    public let isDisabled: Bool

    /// The identity of the option, which is its value.
    public var id: Value { value }

    /// An option.
    ///
    /// - Parameters:
    ///   - title: The label, already localised.
    ///   - value: The value the selection takes when the option is chosen. Unique in the field.
    ///   - systemImage: An SF Symbol shown before the label in the menu.
    ///   - isDisabled: Whether the option cannot be chosen.
    public init(_ title: String, value: Value, systemImage: String? = nil, isDisabled: Bool = false) {
        self.title = title
        self.value = value
        self.systemImage = systemImage
        self.isDisabled = isDisabled
    }
}

extension MonarchSelectOption: Sendable where Value: Sendable {}
