/// Replacements for the built-in strings of ``MonarchMergeDialog``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchMergeDialogLabels: Sendable {
    /// The title. Built in: "Merge records".
    public var title: String?
    /// The line under the title. Built in: "Choose the record to keep and
    /// which value to keep for each field. The other records are merged into it."
    public var description: String?
    /// The heading of the record choice. Built in: "Record to keep".
    public var keep: String?
    /// The badge on the kept record. Built in: "Kept".
    public var kept: String?
    /// The heading of the field choices. Built in: "Values to keep".
    public var values: String?
    /// The source under each value. Receives the record's title. Built in: "from Acme Ltd".
    public var from: (@Sendable (String) -> String)?
    /// The heading of fields that match in every record. Built in: "Same in every record".
    public var same: String?
    /// Shown for an empty value. Built in: "Empty".
    public var empty: String?
    /// The heading of the consequences. Built in: "What happens".
    public var consequences: String?
    /// The confirm button. Receives the number of records. Built in: "Merge 2 records".
    public var confirm: (@Sendable (Int) -> String)?
    /// The cancel button. Built in: "Cancel".
    public var cancel: String?
    /// The error when the merge fails without a description. Built in: "The
    /// records could not be merged. Nothing was changed."
    public var error: String?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        title: String? = nil,
        description: String? = nil,
        keep: String? = nil,
        kept: String? = nil,
        values: String? = nil,
        from: (@Sendable (String) -> String)? = nil,
        same: String? = nil,
        empty: String? = nil,
        consequences: String? = nil,
        confirm: (@Sendable (Int) -> String)? = nil,
        cancel: String? = nil,
        error: String? = nil
    ) {
        self.title = title
        self.description = description
        self.keep = keep
        self.kept = kept
        self.values = values
        self.from = from
        self.same = same
        self.empty = empty
        self.consequences = consequences
        self.confirm = confirm
        self.cancel = cancel
        self.error = error
    }
}
