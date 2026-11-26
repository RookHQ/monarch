/// Built-in strings of the prompt template picker, from the `MonarchAiPromptTemplatePicker` strings table.
enum MonarchAiPromptTemplatePickerStrings {
    private static let table = "MonarchAiPromptTemplatePicker"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.aiPromptTemplatePicker." + key, table: table)
    }

    /// The gallery title.
    static var title: String { string("title") }
    /// The search prompt.
    static var search: String { string("search") }
    /// The category filter name.
    static var category: String { string("category") }
    /// The every-category entry.
    static var allCategories: String { string("allCategories") }
    /// The template heading.
    static var template: String { string("template") }
    /// The preview heading.
    static var preview: String { string("preview") }
    /// The use button.
    static var use: String { string("use") }
    /// The button that opens the chosen template.
    static var next: String { string("next") }
    /// The hint shown until a template is chosen.
    static var pickFirst: String { string("pickFirst") }
    /// The cancel button.
    static var cancel: String { string("cancel") }
    /// The optional note.
    static var optional: String { string("optional") }
    /// The missing value error.
    static func missing(_ label: String) -> String {
        MonarchStrings.string("monarch.aiPromptTemplatePicker.missing", table: table, label.lowercased())
    }
    /// The empty title.
    static var empty: String { string("empty") }
    /// The no results title.
    static func noResults(_ query: String) -> String {
        MonarchStrings.string("monarch.aiPromptTemplatePicker.noResults", table: table, query)
    }
}
