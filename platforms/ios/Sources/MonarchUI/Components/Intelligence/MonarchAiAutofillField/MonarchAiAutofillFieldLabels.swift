/// Replacements for the built-in strings of ``MonarchAiAutofillField``.
///
/// Every property is `nil` by default, which keeps the built-in string.
public struct MonarchAiAutofillFieldLabels: Sendable {
    /// The badge text of a state. Built in: "AI suggested", "AI filled", "Edited by you".
    public var badge: (@Sendable (MonarchAiAutofillState) -> String)?
    /// The title of the accept button. Built in: "Accept".
    public var accept: String?
    /// The title of the clear button. Built in: "Clear".
    public var clear: String?
    /// The spoken name of the accept button, from the field name. Built in: "Accept suggested Company".
    public var acceptField: (@Sendable (String) -> String)?
    /// The spoken name of the clear button, from the field name. Built in: "Clear suggested Company".
    public var clearField: (@Sendable (String) -> String)?
    /// The source line, from the source. Built in: "From invoice.pdf".
    public var source: (@Sendable (String) -> String)?
    /// The line shown once edited, from the AI value. Built in: "AI suggested: Rook Labs".
    public var original: (@Sendable (String) -> String)?
    /// Announced after Accept. Built in: "Suggestion accepted".
    public var accepted: String?
    /// Announced after Clear. Built in: "Suggestion cleared".
    public var cleared: String?

    /// Replacements for built-in strings. Pass only the ones to replace.
    ///
    /// - Parameters:
    ///   - badge: The badge text of a state.
    ///   - accept: The title of the accept button.
    ///   - clear: The title of the clear button.
    ///   - acceptField: The spoken name of the accept button, from the field name.
    ///   - clearField: The spoken name of the clear button, from the field name.
    ///   - source: The source line, from the source.
    ///   - original: The line shown once edited, from the AI value.
    ///   - accepted: Announced after Accept.
    ///   - cleared: Announced after Clear.
    public init(
        badge: (@Sendable (MonarchAiAutofillState) -> String)? = nil,
        accept: String? = nil,
        clear: String? = nil,
        acceptField: (@Sendable (String) -> String)? = nil,
        clearField: (@Sendable (String) -> String)? = nil,
        source: (@Sendable (String) -> String)? = nil,
        original: (@Sendable (String) -> String)? = nil,
        accepted: String? = nil,
        cleared: String? = nil
    ) {
        self.badge = badge
        self.accept = accept
        self.clear = clear
        self.acceptField = acceptField
        self.clearField = clearField
        self.source = source
        self.original = original
        self.accepted = accepted
        self.cleared = cleared
    }
}
