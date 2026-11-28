/// How a status badge is drawn.
public enum MonarchStatusBadgeVariant: Sendable, Hashable, CaseIterable {
    /// A tinted capsule. The default.
    case subtle
    /// A bordered capsule with no fill, for dense tables.
    case outline
    /// A filled capsule, for the one status that matters most on a screen.
    case solid
    /// A coloured dot and plain text, the quietest form.
    case dot
}
