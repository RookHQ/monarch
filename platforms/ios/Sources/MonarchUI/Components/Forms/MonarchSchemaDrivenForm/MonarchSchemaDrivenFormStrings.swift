import Foundation

/// Built-in strings of ``MonarchSchemaDrivenForm``, from the `MonarchSchemaDrivenForm` strings table.
enum MonarchSchemaDrivenFormStrings {
    private static let table = "MonarchSchemaDrivenForm"

    /// The string for a key, such as `submit`.
    static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.schemaDrivenForm.\(key)", table: table)
    }

    /// Every key of the table, for the test that each one resolves.
    static let keys = [
        "submit", "submitFailed", "empty", "saving", "discard", "discardTitle", "discardMessage", "discardConfirm",
        "keepEditing",
    ]
}

/// The strings a ``MonarchSchemaDrivenForm`` shows and speaks: each override from the labels, or the built-in
/// string.
struct MonarchSchemaDrivenFormText {
    let labels: MonarchSchemaDrivenFormLabels

    private func string(_ override: String?, _ key: String) -> String {
        override ?? MonarchSchemaDrivenFormStrings.string(key)
    }

    var submit: String { string(labels.submit, "submit") }
    var submitFailed: String { string(labels.submitFailed, "submitFailed") }
    var empty: String { string(labels.empty, "empty") }
    var saving: String { string(labels.saving, "saving") }
    var discard: String { string(labels.discard, "discard") }
    var discardTitle: String { string(labels.discardTitle, "discardTitle") }
    var discardMessage: String { string(labels.discardMessage, "discardMessage") }
    var discardConfirm: String { string(labels.discardConfirm, "discardConfirm") }
    var keepEditing: String { string(labels.keepEditing, "keepEditing") }
    /// The yes of a yes-or-no answer in the read-only view, shared with ``MonarchFieldRenderer``.
    var yes: String { labels.fields.yes ?? MonarchFieldRendererStrings.string("yes") }
    /// The no of a yes-or-no answer in the read-only view, shared with ``MonarchFieldRenderer``.
    var no: String { labels.fields.no ?? MonarchFieldRendererStrings.string("no") }
}
