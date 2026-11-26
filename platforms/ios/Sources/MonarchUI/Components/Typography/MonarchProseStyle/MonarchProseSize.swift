/// The base size of long-form text. Headings, code and spacing scale with it.
public enum MonarchProseSize: Sendable, Hashable, CaseIterable {
    /// Small text, for comments, chat and side panels.
    case sm
    /// The size for documents. The default.
    case md
    /// Large text, for long reads.
    case lg
}
