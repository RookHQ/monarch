/// The kind of answer ``MonarchAiResponseSkeleton`` stands in for.
public enum MonarchAiResponseSkeletonShape: Sendable, Hashable, CaseIterable {
    /// Paragraph lines with a shorter last line. The default.
    case text
    /// A column of card outlines, each with a title bar and two lines.
    case cards
    /// A header row and body rows of equal columns.
    case table
}
