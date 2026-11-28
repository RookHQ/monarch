/// Replacements for the built-in strings of ``MonarchAiSummaryCard``. Every
/// value is optional; leave one out to keep the built-in English text.
@_spi(Experimental) public struct MonarchAiSummaryCardLabels: Sendable {
    /// The provenance label above the title.
    public var aiSummary: String
    /// The refresh button.
    public var refresh: String
    /// The age line, given a relative time (shown) or a date and time (read by VoiceOver).
    public var generated: @Sendable (_ time: String) -> String
    /// Shown and announced while a summary is being written.
    public var generating: String
    /// Announced when the summary has been written.
    public var updated: String
    /// The stale warning.
    public var stale: String
    /// The error title.
    public var error: String
    /// Shown when there is no summary and nothing is being written.
    public var empty: String
    /// The heading of the sources.
    public var sources: String

    /// Labels with optional replacements.
    ///
    /// - Parameters:
    ///   - aiSummary: Replaces "AI summary".
    ///   - refresh: Replaces "Refresh".
    ///   - generated: Replaces "Generated …".
    ///   - generating: Replaces "Generating summary…".
    ///   - updated: Replaces "Summary updated.".
    ///   - stale: Replaces the stale warning.
    ///   - error: Replaces "The summary couldn’t be generated.".
    ///   - empty: Replaces "No summary yet.".
    ///   - sources: Replaces "Sources".
    public init(
        aiSummary: String? = nil,
        refresh: String? = nil,
        generated: (@Sendable (_ time: String) -> String)? = nil,
        generating: String? = nil,
        updated: String? = nil,
        stale: String? = nil,
        error: String? = nil,
        empty: String? = nil,
        sources: String? = nil
    ) {
        self.aiSummary = aiSummary ?? MonarchAiSummaryCardStrings.aiSummary
        self.refresh = refresh ?? MonarchAiSummaryCardStrings.refresh
        self.generated = generated ?? { MonarchAiSummaryCardStrings.generated($0) }
        self.generating = generating ?? MonarchAiSummaryCardStrings.generating
        self.updated = updated ?? MonarchAiSummaryCardStrings.updated
        self.stale = stale ?? MonarchAiSummaryCardStrings.stale
        self.error = error ?? MonarchAiSummaryCardStrings.error
        self.empty = empty ?? MonarchAiSummaryCardStrings.empty
        self.sources = sources ?? MonarchAiSummaryCardStrings.sources
    }
}
