/// Built-in strings of ``MonarchMergeDialog``, from the `MonarchMergeDialog` strings table.
enum MonarchMergeDialogStrings {
    private static let table = "MonarchMergeDialog"

    /// The title.
    static var title: String { MonarchStrings.string("monarch.mergeDialog.title", table: table) }
    /// The line under the title.
    static var description: String { MonarchStrings.string("monarch.mergeDialog.description", table: table) }
    /// The heading of the record choice.
    static var keep: String { MonarchStrings.string("monarch.mergeDialog.keep", table: table) }
    /// The badge on the kept record.
    static var kept: String { MonarchStrings.string("monarch.mergeDialog.kept", table: table) }
    /// The heading of the field choices.
    static var values: String { MonarchStrings.string("monarch.mergeDialog.values", table: table) }
    /// The source under each value.
    static func from(_ record: String) -> String {
        MonarchStrings.string("monarch.mergeDialog.from", table: table, record)
    }
    /// The heading of fields that match.
    static var same: String { MonarchStrings.string("monarch.mergeDialog.same", table: table) }
    /// Shown for an empty value.
    static var empty: String { MonarchStrings.string("monarch.mergeDialog.empty", table: table) }
    /// The heading of the consequences.
    static var consequences: String { MonarchStrings.string("monarch.mergeDialog.consequences", table: table) }
    /// The confirm button.
    static func confirm(_ count: Int) -> String {
        MonarchStrings.string("monarch.mergeDialog.confirm", table: table, count)
    }
    /// The cancel button.
    static var cancel: String { MonarchStrings.string("monarch.mergeDialog.cancel", table: table) }
    /// The error when the merge fails.
    static var error: String { MonarchStrings.string("monarch.mergeDialog.error", table: table) }
}
