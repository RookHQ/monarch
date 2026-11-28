/// Replacements for the built-in strings of ``MonarchTagInput``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchTagInputLabels: Sendable {
    /// Shown when a tag is already in the list. Receives the tag. Built in:
    /// "“Research” is already added."
    public var duplicate: (@Sendable (String) -> String)?
    /// Shown when the list is full. Receives the maximum. Built in: "You can
    /// add up to 5."
    public var maximum: (@Sendable (Int) -> String)?
    /// Announced after a tag is added. Receives the tag. Built in: "Added
    /// Research."
    public var added: (@Sendable (String) -> String)?
    /// Announced after a tag is removed. Receives the tag. Built in:
    /// "Removed Research."
    public var removed: (@Sendable (String) -> String)?
    /// The spoken name of a tag's remove button. Receives the tag. Built in:
    /// "Remove Research", from ``MonarchTag``.
    public var remove: (@Sendable (String) -> String)?
    /// The spoken name of a suggestion button. Receives the suggestion.
    /// Built in: "Add Research".
    public var addSuggestion: (@Sendable (String) -> String)?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        duplicate: (@Sendable (String) -> String)? = nil,
        maximum: (@Sendable (Int) -> String)? = nil,
        added: (@Sendable (String) -> String)? = nil,
        removed: (@Sendable (String) -> String)? = nil,
        remove: (@Sendable (String) -> String)? = nil,
        addSuggestion: (@Sendable (String) -> String)? = nil
    ) {
        self.duplicate = duplicate
        self.maximum = maximum
        self.added = added
        self.removed = removed
        self.remove = remove
        self.addSuggestion = addSuggestion
    }
}
