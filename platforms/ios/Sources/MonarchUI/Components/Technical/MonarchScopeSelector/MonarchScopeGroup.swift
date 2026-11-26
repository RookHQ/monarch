/// The scopes of one resource in a ``MonarchScopeSelector``, such as one API.
public struct MonarchScopeGroup: Identifiable, Hashable, Sendable {
    /// A stable id, used for the expanded state and auto-selection.
    public let id: String
    /// The group name, such as the API name, already localised.
    public let label: String
    /// Secondary text under the name, such as the API's owner.
    public let description: String?
    /// An SF Symbol before the name. Decorative: the label names the group.
    public let systemImage: String?
    /// The scopes the person may grant, already filtered by the product.
    public let scopes: [MonarchScopeOption]
    /// Whether the scopes are still loading.
    public let isLoading: Bool
    /// Shown instead of the count when the group offers no scopes.
    public let emptyMessage: String?

    /// A group of scopes.
    ///
    /// - Parameters:
    ///   - id: A stable id.
    ///   - label: The group name, already localised.
    ///   - description: Secondary text under the name.
    ///   - systemImage: An SF Symbol before the name.
    ///   - scopes: The scopes the person may grant.
    ///   - isLoading: Whether the scopes are still loading.
    ///   - emptyMessage: Shown instead of the count when the group offers no scopes.
    public init(
        id: String,
        label: String,
        description: String? = nil,
        systemImage: String? = nil,
        scopes: [MonarchScopeOption],
        isLoading: Bool = false,
        emptyMessage: String? = nil
    ) {
        self.id = id
        self.label = label
        self.description = description
        self.systemImage = systemImage
        self.scopes = scopes
        self.isLoading = isLoading
        self.emptyMessage = emptyMessage
    }
}
