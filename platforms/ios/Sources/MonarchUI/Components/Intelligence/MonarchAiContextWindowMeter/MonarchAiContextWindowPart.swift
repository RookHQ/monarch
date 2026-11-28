/// One part of what fills the context window, such as "Instructions" or
/// "Attachments", for ``MonarchAiContextWindowMeter``.
public struct MonarchAiContextWindowPart: Sendable, Hashable, Identifiable {
    /// A stable id. The label when none is given.
    public let id: String
    /// What the part is, already localised.
    public let label: String
    /// The tokens it takes. A negative count is drawn as zero.
    public let tokens: Int

    /// A part of the context window.
    ///
    /// - Parameters:
    ///   - label: What the part is, already localised.
    ///   - id: A stable id. The label when `nil`.
    ///   - tokens: The tokens it takes.
    public init(_ label: String, id: String? = nil, tokens: Int) {
        self.id = id ?? label
        self.label = label
        self.tokens = tokens
    }
}
