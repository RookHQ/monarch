/// Replacements for the built-in strings of ``MonarchInsightStrip``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchInsightStripLabels: Sendable {
    /// The title of the button that reveals the hidden insights. Receives
    /// how many are hidden. Built in: "Show 3 more".
    public var showMore: (@Sendable (Int) -> String)?
    /// The title of the button that hides them again. Built in: "Show fewer".
    public var showLess: String?
    /// Shown when there are no insights. Built in: "Nothing needs attention
    /// right now."
    public var empty: String?
    /// The spoken name of the loading placeholder. Built in: "Loading insights".
    public var loading: String?
    /// The title of the retry button. Built in: "Try again".
    public var retry: String?
    /// The spoken name of the dismiss button. Built in: "Dismiss insight".
    public var dismiss: String?
    /// The label of the evidence section. Built in: "Show evidence".
    public var evidence: String?
    /// The visible label of a rule-based insight. Built in: "Rule-based".
    public var ruleOrigin: String?
    /// The visible label of an insight from a model. Built in: "AI insight".
    public var aiOrigin: String?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        showMore: (@Sendable (Int) -> String)? = nil,
        showLess: String? = nil,
        empty: String? = nil,
        loading: String? = nil,
        retry: String? = nil,
        dismiss: String? = nil,
        evidence: String? = nil,
        ruleOrigin: String? = nil,
        aiOrigin: String? = nil
    ) {
        self.showMore = showMore
        self.showLess = showLess
        self.empty = empty
        self.loading = loading
        self.retry = retry
        self.dismiss = dismiss
        self.evidence = evidence
        self.ruleOrigin = ruleOrigin
        self.aiOrigin = aiOrigin
    }
}
