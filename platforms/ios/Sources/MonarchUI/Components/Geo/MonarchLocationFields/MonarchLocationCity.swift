/// A city from a ``MonarchLocationAdapter``.
public struct MonarchLocationCity: Identifiable, Hashable, Sendable {
    /// The code to store, such as the source's place id.
    public let id: String
    /// The city's name, already localised.
    public let name: String
    /// Secondary text in the list, such as the region the city is in.
    public let description: String?
    /// The region the city belongs to. It fills in the region when the city is chosen before one.
    public let region: MonarchRegion?

    /// A city option.
    ///
    /// - Parameters:
    ///   - id: The code to store.
    ///   - name: The city's name.
    ///   - description: Secondary text in the list.
    ///   - region: The region the city belongs to.
    public init(id: String, name: String, description: String? = nil, region: MonarchRegion? = nil) {
        self.id = id
        self.name = name
        self.description = description
        self.region = region
    }
}
