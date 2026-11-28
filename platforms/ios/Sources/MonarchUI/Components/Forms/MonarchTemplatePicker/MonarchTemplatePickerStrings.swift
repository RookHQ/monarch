/// Built-in strings of the template picker, from the `MonarchTemplatePicker` strings table.
enum MonarchTemplatePickerStrings {
    private static let table = "MonarchTemplatePicker"

    /// The name of the blank card.
    static var blank: String { MonarchStrings.string("monarch.templatePicker.blank", table: table) }
    /// The spoken name of the category filters.
    static var categories: String { MonarchStrings.string("monarch.templatePicker.categories", table: table) }
    /// The filter for every category.
    static var allCategories: String { MonarchStrings.string("monarch.templatePicker.allCategories", table: table) }

    /// The number of templates shown.
    static func results(_ count: Int) -> String {
        let key = count == 1 ? "monarch.templatePicker.resultsOne" : "monarch.templatePicker.results"
        return MonarchStrings.string(key, table: table, count)
    }

    /// Shown when nothing matches the search.
    static func noResults(_ query: String) -> String {
        MonarchStrings.string("monarch.templatePicker.noResults", table: table, query)
    }
}
