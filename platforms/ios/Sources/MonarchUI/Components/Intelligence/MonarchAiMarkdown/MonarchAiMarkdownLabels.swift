/// Replacements for the strings ``MonarchAiMarkdown`` adds around the
/// content. Every value is optional; leave one out to keep the built-in
/// English text.
public struct MonarchAiMarkdownLabels: Sendable {
    /// The name VoiceOver reads for a table.
    public var table: String
    /// The size VoiceOver reads after a table's name, given its rows (header included) and columns.
    public var tableSize: @Sendable (_ rows: Int, _ columns: Int) -> String
    /// The heading of the footnotes listed at the end.
    public var footnotes: String
    /// The name of a task-list check mark; its state is read after it.
    public var task: String
    /// The text shown after a link whose text does not name its host.
    public var linkHost: @Sendable (_ host: String) -> String

    /// Labels with optional replacements.
    ///
    /// - Parameters:
    ///   - table: Replaces "Table".
    ///   - tableSize: Replaces "3 rows, 2 columns".
    ///   - footnotes: Replaces "Footnotes".
    ///   - task: Replaces "Task".
    ///   - linkHost: Replaces "(example.com)".
    public init(
        table: String? = nil,
        tableSize: (@Sendable (_ rows: Int, _ columns: Int) -> String)? = nil,
        footnotes: String? = nil,
        task: String? = nil,
        linkHost: (@Sendable (_ host: String) -> String)? = nil
    ) {
        self.table = table ?? MonarchAiMarkdownStrings.table
        self.tableSize = tableSize ?? { MonarchAiMarkdownStrings.tableSize(rows: $0, columns: $1) }
        self.footnotes = footnotes ?? MonarchAiMarkdownStrings.footnotes
        self.task = task ?? MonarchAiMarkdownStrings.task
        self.linkHost = linkHost ?? { MonarchAiMarkdownStrings.linkHost($0) }
    }
}
