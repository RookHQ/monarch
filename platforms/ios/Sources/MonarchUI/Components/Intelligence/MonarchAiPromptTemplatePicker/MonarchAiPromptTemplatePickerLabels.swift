/// Replacements for the built-in strings of ``MonarchAiPromptTemplatePicker``.
/// Every value is optional; leave one out to keep the built-in English text.
public struct MonarchAiPromptTemplatePickerLabels: Sendable {
    /// The navigation title of the gallery.
    public var title: String
    /// The prompt of the search field.
    public var search: String
    /// The name of the category filter.
    public var category: String
    /// The filter entry that shows every category.
    public var allCategories: String
    /// The heading of the template body on the detail page.
    public var template: String
    /// The heading of the filled prompt on the detail page.
    public var preview: String
    /// The button that hands the prompt to the app.
    public var use: String
    /// The cancel button.
    public var cancel: String
    /// The button under the list that opens the chosen template.
    public var next: String
    /// The hint under the list until a template is chosen.
    public var pickFirst: String
    /// The note after an optional field's label.
    public var optional: String
    /// The error under an empty required field, from its label.
    public var missing: @Sendable (_ label: String) -> String
    /// The title shown when there are no templates.
    public var empty: String
    /// The title shown when the search matches nothing, from the query.
    public var noResults: @Sendable (_ query: String) -> String

    /// Labels with optional replacements.
    ///
    /// - Parameters:
    ///   - title: Replaces "Prompt templates".
    ///   - search: Replaces "Search templates".
    ///   - category: Replaces "Category".
    ///   - allCategories: Replaces "All".
    ///   - template: Replaces "Template".
    ///   - preview: Replaces "Prompt preview".
    ///   - use: Replaces "Use template".
    ///   - cancel: Replaces "Cancel".
    ///   - next: Replaces "Continue".
    ///   - pickFirst: Replaces "Choose a template to continue.".
    ///   - optional: Replaces "Optional".
    ///   - missing: Replaces "Enter …".
    ///   - empty: Replaces "No templates yet".
    ///   - noResults: Replaces "No templates match “…”".
    public init(
        title: String? = nil,
        search: String? = nil,
        category: String? = nil,
        allCategories: String? = nil,
        template: String? = nil,
        preview: String? = nil,
        use: String? = nil,
        cancel: String? = nil,
        next: String? = nil,
        pickFirst: String? = nil,
        optional: String? = nil,
        missing: (@Sendable (_ label: String) -> String)? = nil,
        empty: String? = nil,
        noResults: (@Sendable (_ query: String) -> String)? = nil
    ) {
        typealias Strings = MonarchAiPromptTemplatePickerStrings
        self.title = title ?? Strings.title
        self.search = search ?? Strings.search
        self.category = category ?? Strings.category
        self.allCategories = allCategories ?? Strings.allCategories
        self.template = template ?? Strings.template
        self.preview = preview ?? Strings.preview
        self.use = use ?? Strings.use
        self.cancel = cancel ?? Strings.cancel
        self.next = next ?? Strings.next
        self.pickFirst = pickFirst ?? Strings.pickFirst
        self.optional = optional ?? Strings.optional
        self.missing = missing ?? { Strings.missing($0) }
        self.empty = empty ?? Strings.empty
        self.noResults = noResults ?? { Strings.noResults($0) }
    }
}
