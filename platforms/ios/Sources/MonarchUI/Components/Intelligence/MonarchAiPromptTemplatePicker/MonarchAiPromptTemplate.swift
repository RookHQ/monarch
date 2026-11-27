/// A reusable prompt for ``MonarchAiPromptTemplatePicker``.
public struct MonarchAiPromptTemplate: Identifiable, Sendable, Hashable {
    /// A stable id.
    public let id: String
    /// A short name. Already localised.
    public let title: String
    /// The prompt, with `{{name}}` blanks.
    public let body: String
    /// What the template is for.
    public let description: String?
    /// The id of a category.
    public let category: String?
    /// Extra words the search matches.
    public let keywords: [String]
    /// The blanks, in form order. When `nil` they are read from the body,
    /// and all of them are required.
    public let variables: [MonarchAiPromptTemplateVariable]?

    /// A prompt template.
    ///
    /// - Parameters:
    ///   - id: A stable id.
    ///   - title: A short name, already localised.
    ///   - body: The prompt, with `{{name}}` blanks.
    ///   - description: What the template is for.
    ///   - category: The id of a category.
    ///   - keywords: Extra words the search matches.
    ///   - variables: The blanks in form order; read from the body when `nil`.
    public init(
        id: String,
        title: String,
        body: String,
        description: String? = nil,
        category: String? = nil,
        keywords: [String] = [],
        variables: [MonarchAiPromptTemplateVariable]? = nil
    ) {
        self.id = id
        self.title = title
        self.body = body
        self.description = description
        self.category = category
        self.keywords = keywords
        self.variables = variables
    }

    /// The distinct `{{name}}` blanks of a body, in order of first use.
    /// Spaces inside the braces are allowed.
    public static func variableNames(in body: String) -> [String] {
        var names: [String] = []
        for match in body.matches(of: MonarchAiPromptTemplatePickerTokens.blank) {
            let name = String(match.output.1)
            if !names.contains(name) { names.append(name) }
        }
        return names
    }

    /// Fills a body: each `{{name}}` becomes its value. A blank without a
    /// value is left as it is, so nothing is dropped silently; an empty
    /// value removes the blank.
    public static func fill(_ body: String, values: [String: String]) -> String {
        body.replacing(MonarchAiPromptTemplatePickerTokens.blank) { match in
            values[String(match.output.1)] ?? String(match.output.0)
        }
    }

    /// The blanks of the template: its `variables`, or the ones read from
    /// the body, all required, with labels made from their names.
    public var resolvedVariables: [MonarchAiPromptTemplateVariable] {
        if let variables { return variables }
        return Self.variableNames(in: body).map {
            MonarchAiPromptTemplateVariable(
                $0,
                label: MonarchAiPromptTemplatePickerTokens.humanize($0),
                isRequired: true
            )
        }
    }
}
