/// How strongly a badge is drawn.
public enum MonarchBadgeVariant: Sendable, Hashable, CaseIterable {
    /// A tinted background. The default.
    case subtle
    /// A filled background, for the most important status on a screen.
    case solid
    /// A border and no fill.
    case outline
}
