/// How a ``MonarchDnsRecordTable`` lays its records out.
public enum MonarchDnsRecordTableLayout: Sendable, Hashable, CaseIterable {
    /// Cards in a compact width or at accessibility text sizes, a table
    /// in a regular width.
    case automatic
    /// One card per record. Fits any width and any text size.
    case cards
    /// A compact grid with one row per record, for wide screens.
    case table
}
