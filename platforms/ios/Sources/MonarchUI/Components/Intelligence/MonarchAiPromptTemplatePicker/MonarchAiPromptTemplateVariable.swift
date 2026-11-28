/// One blank in a prompt template, written `{{name}}` in its body, for
/// ``MonarchAiPromptTemplatePicker``.
public struct MonarchAiPromptTemplateVariable: Identifiable, Sendable, Hashable {
    /// The name used in the body: `{{name}}`.
    public let name: String
    /// The field label, such as "Customer name". Already localised.
    public let label: String
    /// Whether the blank must be filled before the template can be used.
    public let isRequired: Bool
    /// An example value shown in the empty field.
    public let placeholder: String?
    /// Help under the field.
    public let helper: String?
    /// Whether the field takes several lines.
    public let isMultiline: Bool
    /// The value the field starts with.
    public let defaultValue: String?

    /// The id: the name.
    public var id: String { name }

    /// A template blank.
    ///
    /// - Parameters:
    ///   - name: The name used in the body.
    ///   - label: The field label, already localised.
    ///   - isRequired: Whether it must be filled.
    ///   - placeholder: An example value shown in the empty field.
    ///   - helper: Help under the field.
    ///   - isMultiline: Whether the field takes several lines.
    ///   - defaultValue: The value the field starts with.
    public init(
        _ name: String,
        label: String,
        isRequired: Bool = false,
        placeholder: String? = nil,
        helper: String? = nil,
        isMultiline: Bool = false,
        defaultValue: String? = nil
    ) {
        self.name = name
        self.label = label
        self.isRequired = isRequired
        self.placeholder = placeholder
        self.helper = helper
        self.isMultiline = isMultiline
        self.defaultValue = defaultValue
    }
}
