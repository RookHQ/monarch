/// How an inline error row is drawn.
public enum MonarchInlineErrorRetryVariant: Sendable, Hashable, CaseIterable {
    /// A compact row with no surface, inside a card or section. The default.
    case inline
    /// A tinted, bordered strip, for example above stale data that is still shown.
    case banner
}
