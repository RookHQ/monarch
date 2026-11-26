/// How a code fragment in running text is drawn.
public enum MonarchInlineCodeVariant: Sendable, Hashable, CaseIterable {
    /// The monospaced face on a muted background. The default.
    case subtle
    /// The monospaced face only, for code inside dense rows and cells.
    case plain
}
