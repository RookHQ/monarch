/// A problem with one row of an import, optionally tied to a field.
public struct MonarchPreviewRowIssue: Sendable, Hashable {
    /// The field the problem is in, already localised, for example "Email".
    public var field: String?
    /// What is wrong, already localised.
    public var message: String

    /// A problem with a row.
    ///
    /// - Parameters:
    ///   - message: What is wrong, already localised.
    ///   - field: The field the problem is in, if any.
    public init(_ message: String, field: String? = nil) {
        self.message = message
        self.field = field
    }
}
