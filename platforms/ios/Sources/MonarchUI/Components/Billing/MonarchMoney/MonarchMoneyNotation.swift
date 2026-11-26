/// How long an amount is written.
public enum MonarchMoneyNotation: Sendable, Hashable, CaseIterable {
    /// Every digit: $1,250,000.00.
    case standard
    /// Shortened: $1.3M. VoiceOver reads the full amount.
    case compact
}
