/// One role a person can be given, in a ``MonarchRolePicker``.
public struct MonarchRoleOption<Value: Hashable>: Identifiable {
    /// The value the selection takes when the role is chosen, such as `"admin"`.
    public let value: Value
    /// The name of the role, such as "Admin". It is the spoken name of the choice.
    public let title: String
    /// What someone with this role can do, in plain words.
    public let description: String?
    /// A short list of things the role can do, shown under the description in the cards layout.
    public let capabilities: [String]
    /// Whether the role cannot be chosen here.
    public let isDisabled: Bool
    /// Why it cannot be chosen, such as "Only owners can make someone an owner".
    public let disabledReason: String?

    /// The identity of the role, which is its value.
    public var id: Value { value }

    /// A role.
    ///
    /// - Parameters:
    ///   - value: The value the selection takes when the role is chosen.
    ///   - title: The name of the role, already localised.
    ///   - description: What someone with this role can do, in plain words.
    ///   - capabilities: A short list of things the role can do.
    ///   - isDisabled: Whether the role cannot be chosen here.
    ///   - disabledReason: Why it cannot be chosen. Shown only when `isDisabled` is true.
    public init(
        _ value: Value,
        title: String,
        description: String? = nil,
        capabilities: [String] = [],
        isDisabled: Bool = false,
        disabledReason: String? = nil
    ) {
        self.value = value
        self.title = title
        self.description = description
        self.capabilities = capabilities
        self.isDisabled = isDisabled
        self.disabledReason = disabledReason
    }
}

extension MonarchRoleOption: Sendable where Value: Sendable {}
