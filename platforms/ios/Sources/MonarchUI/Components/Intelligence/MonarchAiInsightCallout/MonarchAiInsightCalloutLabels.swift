/// Replacements for the built-in strings of ``MonarchAiInsightCallout``.
/// Leave a value `nil` to keep the built-in English text.
public struct MonarchAiInsightCalloutLabels: Sendable, Hashable {
    /// Replaces the spoken name of the tone read before the title, such as "Warning".
    public var tone: String?
    /// Replaces "AI insight".
    public var ai: String?
    /// Replaces "Rule-based".
    public var rule: String?
    /// Replaces the title of the evidence disclosure, "Show evidence".
    public var evidence: String?
    /// Replaces the spoken name of the dismiss button, "Dismiss insight".
    public var dismiss: String?

    /// Labels with optional replacements.
    ///
    /// - Parameters:
    ///   - tone: Replaces the spoken tone name read before the title.
    ///   - ai: Replaces "AI insight".
    ///   - rule: Replaces "Rule-based".
    ///   - evidence: Replaces "Show evidence".
    ///   - dismiss: Replaces "Dismiss insight".
    public init(
        tone: String? = nil,
        ai: String? = nil,
        rule: String? = nil,
        evidence: String? = nil,
        dismiss: String? = nil
    ) {
        self.tone = tone
        self.ai = ai
        self.rule = rule
        self.evidence = evidence
        self.dismiss = dismiss
    }
}
