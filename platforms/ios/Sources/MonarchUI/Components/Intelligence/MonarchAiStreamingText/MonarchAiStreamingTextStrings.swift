/// Built-in strings of ``MonarchAiStreamingText``, from the `MonarchAiStreamingText` strings table.
enum MonarchAiStreamingTextStrings {
    private static let table = "MonarchAiStreamingText"

    /// Announced when streaming ends.
    static var complete: String { MonarchStrings.string("monarch.aiStreamingText.complete", table: table) }
    /// Announced in place of a code block.
    static var codeBlock: String { MonarchStrings.string("monarch.aiStreamingText.codeBlock", table: table) }
    /// Announced in place of a table.
    static var tableName: String { MonarchStrings.string("monarch.aiStreamingText.table", table: table) }
}
