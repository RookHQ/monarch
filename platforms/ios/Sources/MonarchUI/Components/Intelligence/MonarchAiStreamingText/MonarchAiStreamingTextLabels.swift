/// Replacements for the strings ``MonarchAiStreamingText`` adds. Every
/// property is `nil` by default, which keeps the built-in string.
public struct MonarchAiStreamingTextLabels: Sendable {
    /// Announced when streaming ends. Built in: "Response complete".
    public var complete: String?
    /// Announced in place of a code block's contents. Built in: "Code block".
    public var codeBlock: String?
    /// Announced in place of a table's contents. Built in: "Table".
    public var table: String?

    /// Replacements for built-in strings. Pass only the ones to replace.
    ///
    /// - Parameters:
    ///   - complete: Replaces "Response complete".
    ///   - codeBlock: Replaces "Code block".
    ///   - table: Replaces "Table".
    public init(complete: String? = nil, codeBlock: String? = nil, table: String? = nil) {
        self.complete = complete
        self.codeBlock = codeBlock
        self.table = table
    }
}
