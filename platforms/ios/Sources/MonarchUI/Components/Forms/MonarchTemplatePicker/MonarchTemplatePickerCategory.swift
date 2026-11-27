/// A category filter of a ``MonarchTemplatePicker``.
public struct MonarchTemplatePickerCategory: Sendable, Hashable, Identifiable {
    /// The id templates refer to.
    public let id: String
    /// The name people see, already localised.
    public let label: String

    /// A category.
    ///
    /// - Parameters:
    ///   - id: The id templates refer to.
    ///   - label: The name people see, already localised.
    public init(id: String, label: String) {
        self.id = id
        self.label = label
    }
}
