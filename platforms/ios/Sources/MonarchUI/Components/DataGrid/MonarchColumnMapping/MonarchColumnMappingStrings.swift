/// Built-in strings of the column mapping, from the `MonarchColumnMapping` strings table.
enum MonarchColumnMappingStrings {
    private static let table = "MonarchColumnMapping"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.columnMapping.\(key)", table: table)
    }

    /// The spoken name of the list.
    static var label: String { string("label") }
    /// The option that leaves a column out.
    static var skip: String { string("skip") }
    /// Added to required fields in the picker.
    static var required: String { string("required") }
    /// The badge of an exact suggestion.
    static var exactMatch: String { string("exactMatch") }
    /// The badge of a close suggestion.
    static var suggested: String { string("suggested") }
    /// Shown when a column has no example values.
    static var noSamples: String { string("noSamples") }
    /// Resets every picker to the suggestions.
    static var useSuggestions: String { string("useSuggestions") }
    /// Leaves every column out.
    static var skipAll: String { string("skipAll") }

    /// The spoken name of a picker.
    static func fieldFor(_ source: String) -> String {
        MonarchStrings.string("monarch.columnMapping.fieldFor", table: table, source)
    }

    /// The summary of mapped columns.
    static func summary(mapped: Int, total: Int) -> String {
        MonarchStrings.string("monarch.columnMapping.summary", table: table, mapped, total)
    }

    /// Required fields no column maps to.
    static func missing(_ fields: String) -> String {
        MonarchStrings.string("monarch.columnMapping.missing", table: table, fields)
    }

    /// A field chosen for more than one column.
    static func duplicate(_ field: String, others: String) -> String {
        MonarchStrings.string("monarch.columnMapping.duplicate", table: table, field, others)
    }
    /// The "Column in file" heading.
    static var columnInFile: String { string("columnInFile") }
    /// The "Example values" heading.
    static var exampleValues: String { string("exampleValues") }
    /// The "Import as" heading.
    static var importAs: String { string("importAs") }
}
