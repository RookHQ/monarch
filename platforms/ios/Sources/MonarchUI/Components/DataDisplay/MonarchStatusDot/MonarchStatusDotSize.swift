/// The size of a status dot and its label.
public enum MonarchStatusDotSize: Sendable, Hashable, CaseIterable {
    /// An 8-point dot with caption text. For table cells and small text.
    case sm
    /// A 10-point dot with label text. Beside body text.
    case md
}
