/// Built-in strings of the document relations, from the `MonarchDocumentRelations` strings table.
enum MonarchDocumentRelationsStrings {
    private static let table = "MonarchDocumentRelations"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.documentRelations.\(key)", table: table)
    }

    private static func string(_ key: String, _ title: String) -> String {
        MonarchStrings.string("monarch.documentRelations.\(key)", table: table, title)
    }

    static var label: String { string("label") }
    static var add: String { string("add") }
    static var sheetTitle: String { string("sheetTitle") }
    static var sheetDescription: String { string("sheetDescription") }
    static var type: String { string("type") }
    static var search: String { string("search") }
    static var results: String { string("results") }
    static var noResults: String { string("noResults") }
    static var cancel: String { string("cancel") }
    static var failed: String { string("failed") }
    static var none: String { string("none") }
    static var relatedTo: String { string("relatedTo") }
    static var parentOf: String { string("parentOf") }
    static var childOf: String { string("childOf") }
    static var supersedes: String { string("supersedes") }
    static var supersededBy: String { string("supersededBy") }
    static var implements: String { string("implements") }
    static var specifiedBy: String { string("specifiedBy") }

    /// The remove button of a relation.
    static func remove(_ title: String) -> String { string("remove", title) }
    /// Announced after adding a relation.
    static func added(_ title: String) -> String { string("added", title) }
    /// Announced after removing a relation.
    static func removed(_ title: String) -> String { string("removed", title) }
}
