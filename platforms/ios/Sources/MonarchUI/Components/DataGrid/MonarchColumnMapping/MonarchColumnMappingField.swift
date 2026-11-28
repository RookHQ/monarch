/// A field of the product that imported columns can be mapped to.
public struct MonarchColumnMappingField: Identifiable, Sendable, Hashable {
    /// A stable id: the value stored in the mapping.
    public let id: String
    /// The name shown in the picker, already localised.
    public let label: String
    /// Some column must be mapped to it.
    public let isRequired: Bool
    /// Help, such as the expected format. Shown under the picker once chosen.
    public let description: String?
    /// Other header names that mean this field ("E-mail", "Mail"), for suggestions.
    public let aliases: [String]

    /// A field.
    ///
    /// - Parameters:
    ///   - id: A stable id.
    ///   - label: The name shown in the picker.
    ///   - isRequired: Some column must be mapped to it.
    ///   - description: Help shown once the field is chosen.
    ///   - aliases: Other header names that mean this field.
    public init(
        id: String,
        label: String,
        isRequired: Bool = false,
        description: String? = nil,
        aliases: [String] = []
    ) {
        self.id = id
        self.label = label
        self.isRequired = isRequired
        self.description = description
        self.aliases = aliases
    }
}
