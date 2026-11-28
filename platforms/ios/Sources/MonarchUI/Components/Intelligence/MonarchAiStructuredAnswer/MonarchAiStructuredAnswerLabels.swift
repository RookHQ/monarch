/// Replacements for the built-in strings of ``MonarchAiStructuredAnswer``.
///
/// Every property is `nil` by default, which keeps the built-in string.
public struct MonarchAiStructuredAnswerLabels: Sendable {
    /// The provenance label above the answer. Built in: "AI answer".
    public var aiAnswer: String?
    /// Read for a field that has not arrived while streaming. Built in: "Still generating".
    public var pending: String?
    /// Read for a field with no value once the answer is complete. Built in: "Not provided".
    public var empty: String?
    /// Announced when streaming ends. Built in: "Response complete."
    public var complete: String?

    /// Replacements for built-in strings. Pass only the ones to replace.
    ///
    /// - Parameters:
    ///   - aiAnswer: Replaces "AI answer".
    ///   - pending: Replaces "Still generating".
    ///   - empty: Replaces "Not provided".
    ///   - complete: Replaces "Response complete."
    public init(aiAnswer: String? = nil, pending: String? = nil, empty: String? = nil, complete: String? = nil) {
        self.aiAnswer = aiAnswer
        self.pending = pending
        self.empty = empty
        self.complete = complete
    }
}
