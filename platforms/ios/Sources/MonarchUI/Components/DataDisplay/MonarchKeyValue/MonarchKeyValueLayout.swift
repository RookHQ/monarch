/// How the pairs of a ``MonarchKeyValue`` are arranged.
public enum MonarchKeyValueLayout: Sendable, Hashable, CaseIterable {
    /// Pairs flow onto more lines when they do not fit. The default.
    case wrap
    /// One pair per line, for narrow cards.
    case stack
}
