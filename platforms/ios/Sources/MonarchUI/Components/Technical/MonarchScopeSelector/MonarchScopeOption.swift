/// One permission of a ``MonarchScopeGroup``, such as `projects:read`.
public struct MonarchScopeOption: Identifiable, Hashable, Sendable {
    /// The scope string sent to the server, such as `projects:read`. Unique across the selector.
    public let value: String
    /// The visible label, already localised. When `nil` the value is shown in the code font.
    public let label: String?
    /// What the scope allows, shown under the label.
    public let description: String?
    /// Whether the scope cannot be changed. Select all leaves it as it is.
    public let isDisabled: Bool

    /// The scope string, which identifies the option.
    public var id: String { value }

    /// A scope.
    ///
    /// - Parameters:
    ///   - value: The scope string sent to the server.
    ///   - label: The visible label; the value in the code font when `nil`.
    ///   - description: What the scope allows.
    ///   - isDisabled: Whether the scope cannot be changed.
    public init(_ value: String, label: String? = nil, description: String? = nil, isDisabled: Bool = false) {
        self.value = value
        self.label = label
        self.description = description
        self.isDisabled = isDisabled
    }
}
