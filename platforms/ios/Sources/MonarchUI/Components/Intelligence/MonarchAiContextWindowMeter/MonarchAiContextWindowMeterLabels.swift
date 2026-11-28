/// Replacements for the built-in strings of ``MonarchAiContextWindowMeter``.
/// Every value is optional; leave one out to keep the built-in English text.
public struct MonarchAiContextWindowMeterLabels: Sendable {
    /// The heading, and the spoken name of the bar.
    public var label: String
    /// The summary line and the bar's spoken value, from the formatted
    /// amount, window size and percentage: "45,000 of 128,000 tokens (35%)".
    public var amount: @Sendable (_ used: String, _ limit: String, _ percent: String) -> String
    /// Wraps the summary when the count is an estimate: "About …".
    public var approximate: @Sendable (_ amount: String) -> String
    /// The spoken name of the legend.
    public var legend: String
    /// The legend entry for tokens no part covers.
    public var other: String
    /// A legend entry's amount, from the formatted count: "12,000 tokens".
    public var tokens: @Sendable (_ formatted: String) -> String
    /// The notice at or above the warning share.
    public var warning: String
    /// The notice when the window is full.
    public var full: String
    /// The title of the summarize button.
    public var summarize: String
    /// The spoken hint of the compact meter, which opens the details.
    public var showDetails: String

    /// Labels with optional replacements.
    ///
    /// - Parameters:
    ///   - label: Replaces "Context window".
    ///   - amount: Replaces "… of … tokens (…)".
    ///   - approximate: Replaces "About …".
    ///   - legend: Replaces "What fills the context window".
    ///   - other: Replaces "Other".
    ///   - tokens: Replaces "… tokens".
    ///   - warning: Replaces the notice near the limit.
    ///   - full: Replaces the notice when the window is full.
    ///   - summarize: Replaces "Summarize earlier messages".
    ///   - showDetails: Replaces "Shows what fills the context window".
    public init(
        label: String? = nil,
        amount: (@Sendable (_ used: String, _ limit: String, _ percent: String) -> String)? = nil,
        approximate: (@Sendable (_ amount: String) -> String)? = nil,
        legend: String? = nil,
        other: String? = nil,
        tokens: (@Sendable (_ formatted: String) -> String)? = nil,
        warning: String? = nil,
        full: String? = nil,
        summarize: String? = nil,
        showDetails: String? = nil
    ) {
        self.label = label ?? MonarchAiContextWindowMeterStrings.label
        self.amount = amount ?? { MonarchAiContextWindowMeterStrings.amount($0, $1, $2) }
        self.approximate = approximate ?? { MonarchAiContextWindowMeterStrings.approximate($0) }
        self.legend = legend ?? MonarchAiContextWindowMeterStrings.legend
        self.other = other ?? MonarchAiContextWindowMeterStrings.other
        self.tokens = tokens ?? { MonarchAiContextWindowMeterStrings.tokens($0) }
        self.warning = warning ?? MonarchAiContextWindowMeterStrings.warning
        self.full = full ?? MonarchAiContextWindowMeterStrings.full
        self.summarize = summarize ?? MonarchAiContextWindowMeterStrings.summarize
        self.showDetails = showDetails ?? MonarchAiContextWindowMeterStrings.showDetails
    }
}
