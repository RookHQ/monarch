/// Replacements for the built-in strings of ``MonarchTemplatePicker``.
///
/// Every property is `nil` by default, which keeps the built-in string.
public struct MonarchTemplatePickerLabels: Sendable {
    /// The spoken name of the category filters. Built in: "Categories".
    public var categories: String?
    /// The filter that shows every category. Built in: "All".
    public var allCategories: String?
    /// The number of templates shown, announced as the filters change. Built in: "3 templates".
    public var results: (@Sendable (Int) -> String)?
    /// Shown when nothing matches the search. Built in: "No templates match “query”."
    public var noResults: (@Sendable (String) -> String)?

    /// Replacements for built-in strings. Pass only the ones to replace.
    public init(
        categories: String? = nil,
        allCategories: String? = nil,
        results: (@Sendable (Int) -> String)? = nil,
        noResults: (@Sendable (String) -> String)? = nil
    ) {
        self.categories = categories
        self.allCategories = allCategories
        self.results = results
        self.noResults = noResults
    }
}
