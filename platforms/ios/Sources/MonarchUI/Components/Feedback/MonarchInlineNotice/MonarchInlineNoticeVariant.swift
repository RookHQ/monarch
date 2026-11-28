/// How an inline notice is drawn.
public enum MonarchInlineNoticeVariant: Sendable, Hashable, CaseIterable {
    /// A tinted strip. The default.
    case subtle
    /// An icon and coloured text only, for tight spots such as under a field
    /// or inside a row.
    case plain
}
