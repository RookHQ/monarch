/// One action edited by a ``MonarchActionEditor``: a type and its parameters.
public struct MonarchRuleAction: Identifiable, Hashable, Sendable {
    /// A stable, unique id.
    public var id: String
    /// The ``MonarchActionType/id`` of its type.
    public var type: String
    /// The parameter values by ``MonarchActionParameter/key``.
    public var parameters: [String: MonarchActionValue]

    /// An action.
    ///
    /// - Parameters:
    ///   - id: A stable, unique id.
    ///   - type: The id of its type.
    ///   - parameters: The parameter values by key.
    public init(id: String, type: String, parameters: [String: MonarchActionValue] = [:]) {
        self.id = id
        self.type = type
        self.parameters = parameters
    }

    /// A new action of a type, with the type's default parameters.
    ///
    /// - Parameters:
    ///   - id: A stable, unique id.
    ///   - type: The type, or `nil` for none yet.
    public init(id: String, type: MonarchActionType?) {
        self.init(id: id, type: type?.id ?? "", parameters: type?.defaults ?? [:])
    }
}
