/// How much harm a wrong answer could do where a ``MonarchAiDisclaimer`` appears.
public enum MonarchAiDisclaimerRisk: Sendable, Hashable, CaseIterable {
    /// A quiet, centred line under a composer or an answer. The default.
    case low
    /// Stronger wording with an information icon.
    case medium
    /// A warning-toned note, for answers people might act on: money, health, legal, deleting data.
    case high
}
