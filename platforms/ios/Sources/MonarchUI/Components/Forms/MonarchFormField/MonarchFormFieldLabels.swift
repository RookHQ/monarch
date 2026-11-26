/// Replacements for the built-in strings of ``MonarchFormField``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchFormFieldLabels: Sendable {
    /// Spoken in the hint of a required field. Built in: "Required".
    public var required: String?
    /// The spoken form of an error message. Receives the message. Built in:
    /// "Error: <message>".
    public var error: (@Sendable (String) -> String)?
    /// Spoken in the hint of a read-only field. Built in: "Read only".
    public var readOnly: String?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        required: String? = nil,
        error: (@Sendable (String) -> String)? = nil,
        readOnly: String? = nil
    ) {
        self.required = required
        self.error = error
        self.readOnly = readOnly
    }
}
