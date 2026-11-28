/// How an event reads at a glance.
///
/// Every status other than `confirmed` is also part of the chip's spoken
/// name, so it never relies on colour or line style alone.
public enum MonarchEventChipStatus: String, Sendable, Hashable, CaseIterable {
    /// Accepted. Drawn solid. The default.
    case confirmed
    /// Not yet accepted. Drawn hollow or dashed.
    case tentative
    /// Called off. Drawn muted, with the title struck through.
    case cancelled
    /// Turned down by the person looking. Drawn muted, with the title struck
    /// through.
    case declined
}
