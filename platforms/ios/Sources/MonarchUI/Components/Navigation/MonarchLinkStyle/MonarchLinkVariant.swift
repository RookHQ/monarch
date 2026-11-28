/// How a link is drawn.
public enum MonarchLinkVariant: Sendable, Hashable, CaseIterable {
    /// The link colour with an underline, for a link beside or under running text. The default.
    case inline
    /// The link colour in a medium weight without an underline, for a link in a list or a footer.
    case standalone
    /// The muted text colour without an underline, for a link of low importance.
    case subtle
}
