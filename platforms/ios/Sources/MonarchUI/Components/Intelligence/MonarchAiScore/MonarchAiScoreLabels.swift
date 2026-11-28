/// Replacements for the built-in strings of ``MonarchAiScore``.
///
/// Every property is `nil` by default, which keeps the built-in string.
public struct MonarchAiScoreLabels: Sendable {
    /// What is scored when no label is given. Built in: "Score".
    public var label: String?
    /// The AI provenance mark beside the label. Built in: "AI score".
    public var aiGenerated: String?
    /// The spoken value, from the formatted score and maximum. Built in: "72 out of 100".
    public var valueText: (@Sendable (String, String) -> String)?
    /// Shown and spoken while there is no score. Built in: "No score yet".
    public var unavailable: String?
    /// The heading of the factors. Built in: "What affects this score".
    public var factors: String?
    /// The spoken effect of a factor, from its formatted size and whether it
    /// raised the score. Built in: "Raised the score by 12", "Lowered the score by 8".
    public var impact: (@Sendable (String, Bool) -> String)?
    /// The spoken effect of a factor with no impact. Built in: "No effect on the score".
    public var noImpact: String?
    /// The summary of the band legend. Built in: "How to read this score".
    public var bands: String?
    /// Marks the band the score is in, in the legend. Built in: "this score".
    public var currentBand: String?
    /// The heading of the factor column. Built in: "Factor".
    public var factorColumn: String?
    /// The heading of the impact column. Built in: "Impact".
    public var impactColumn: String?

    /// Replacements for built-in strings. Pass only the ones to replace.
    public init(
        label: String? = nil,
        aiGenerated: String? = nil,
        valueText: (@Sendable (String, String) -> String)? = nil,
        unavailable: String? = nil,
        factors: String? = nil,
        impact: (@Sendable (String, Bool) -> String)? = nil,
        noImpact: String? = nil,
        bands: String? = nil,
        currentBand: String? = nil,
        factorColumn: String? = nil,
        impactColumn: String? = nil
    ) {
        self.label = label
        self.aiGenerated = aiGenerated
        self.valueText = valueText
        self.unavailable = unavailable
        self.factors = factors
        self.impact = impact
        self.noImpact = noImpact
        self.bands = bands
        self.currentBand = currentBand
        self.factorColumn = factorColumn
        self.impactColumn = impactColumn
    }
}
