/// Built-in strings of the context divider, from the `MonarchAiContextDivider` strings table.
enum MonarchAiContextDividerStrings {
    private static let table = "MonarchAiContextDivider"

    /// The label of a context reset.
    static var contextReset: String { MonarchStrings.string("monarch.aiContextDivider.contextReset", table: table) }
    /// Explains a context reset.
    static var contextResetDescription: String {
        MonarchStrings.string("monarch.aiContextDivider.contextResetDescription", table: table)
    }
    /// The label of a summarized section.
    static var summarized: String { MonarchStrings.string("monarch.aiContextDivider.summarized", table: table) }
    /// The title of the disclosure that reveals the summary.
    static var showSummary: String { MonarchStrings.string("monarch.aiContextDivider.showSummary", table: table) }
    /// Spoken before the summary, so it is identified as written by AI.
    static var summaryPrefix: String { MonarchStrings.string("monarch.aiContextDivider.summaryPrefix", table: table) }
    /// The title of the button that shows the original messages.
    static var expand: String { MonarchStrings.string("monarch.aiContextDivider.expand", table: table) }
}
