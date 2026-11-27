/// A category filter of ``MonarchAiPromptTemplatePicker``.
public struct MonarchAiPromptTemplateCategory: Identifiable, Sendable, Hashable {
    /// The id that templates name in `category`.
    public let id: String
    /// The name shown in the filter. Already localised.
    public let label: String

    /// A category.
    ///
    /// - Parameters:
    ///   - id: The id that templates name.
    ///   - label: The name shown in the filter, already localised.
    public init(id: String, label: String) {
        self.id = id
        self.label = label
    }
}
