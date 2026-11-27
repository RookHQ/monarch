/// A kind of action of a ``MonarchActionEditor``, such as "Set priority" or "Notify agents".
public struct MonarchActionType: Identifiable, Sendable {
    /// The stable value stored on an action.
    public let id: String
    /// The visible name, already localised.
    public let label: String
    /// One line on what it does, shown under the menu.
    public let description: String?
    /// The heading it is listed under in the menu, such as "Assignment".
    public let group: String?
    /// The inputs, in order.
    public let parameters: [MonarchActionParameter]
    /// The parameters a new action of this type starts with.
    public let defaults: [String: MonarchActionValue]
    /// Whether it cannot be chosen now. It stays listed so existing actions keep their type.
    public let isDisabled: Bool
    /// Why it cannot be chosen; shown in the menu and as a warning on actions of this type.
    public let disabledReason: String?
    /// An extra check: returns a message when the parameters are not usable.
    public let validate: (@Sendable ([String: MonarchActionValue]) -> String?)?

    /// An action type.
    ///
    /// - Parameters:
    ///   - id: The stable value stored on an action.
    ///   - label: The visible name, already localised.
    ///   - description: One line on what it does.
    ///   - group: The menu heading it is listed under.
    ///   - parameters: The inputs, in order.
    ///   - defaults: The parameters a new action starts with.
    ///   - isDisabled: Whether it cannot be chosen now.
    ///   - disabledReason: Why it cannot be chosen.
    ///   - validate: An extra check that returns a message when the parameters are not usable.
    public init(
        id: String,
        label: String,
        description: String? = nil,
        group: String? = nil,
        parameters: [MonarchActionParameter] = [],
        defaults: [String: MonarchActionValue] = [:],
        isDisabled: Bool = false,
        disabledReason: String? = nil,
        validate: (@Sendable ([String: MonarchActionValue]) -> String?)? = nil
    ) {
        self.id = id
        self.label = label
        self.description = description
        self.group = group
        self.parameters = parameters
        self.defaults = defaults
        self.isDisabled = isDisabled
        self.disabledReason = disabledReason
        self.validate = validate
    }
}
