/// Replacements for the built-in strings of ``MonarchColumnMapping``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchColumnMappingLabels: Sendable {
    /// The spoken name of the list. Built in: "Column mapping".
    public var label: String?
    /// The spoken name of a picker. Receives the column. Built in: "Field for E-mail".
    public var fieldFor: (@Sendable (String) -> String)?
    /// The option that leaves a column out. Built in: "Don’t import".
    public var skip: String?
    /// Added to required fields in the picker. Built in: "required".
    public var required: String?
    /// The badge of an exact suggestion. Built in: "Matched".
    public var exactMatch: String?
    /// The badge of a close suggestion. Built in: "Suggested".
    public var suggested: String?
    /// Shown when a column has no example values. Built in: "No values".
    public var noSamples: String?
    /// The summary. Receives the mapped and total columns. Built in: "8 of 10 columns mapped".
    public var summary: (@Sendable (Int, Int) -> String)?
    /// Required fields no column maps to. Receives their names, joined. Built in: "Map a column to: Email, Name."
    public var missing: (@Sendable (String) -> String)?
    /// A field chosen for more than one column. Receives the field and the
    /// other columns. Built in: "Email is also chosen for Mail."
    public var duplicate: (@Sendable (String, String) -> String)?
    /// Resets every picker to the suggestions. Built in: "Use suggestions".
    public var useSuggestions: String?
    /// Sets every picker to leave its column out. Built in: "Don’t import any".
    public var skipAll: String?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        label: String? = nil,
        fieldFor: (@Sendable (String) -> String)? = nil,
        skip: String? = nil,
        required: String? = nil,
        exactMatch: String? = nil,
        suggested: String? = nil,
        noSamples: String? = nil,
        summary: (@Sendable (Int, Int) -> String)? = nil,
        missing: (@Sendable (String) -> String)? = nil,
        duplicate: (@Sendable (String, String) -> String)? = nil,
        useSuggestions: String? = nil,
        skipAll: String? = nil
    ) {
        self.label = label
        self.fieldFor = fieldFor
        self.skip = skip
        self.required = required
        self.exactMatch = exactMatch
        self.suggested = suggested
        self.noSamples = noSamples
        self.summary = summary
        self.missing = missing
        self.duplicate = duplicate
        self.useSuggestions = useSuggestions
        self.skipAll = skipAll
    }
}
