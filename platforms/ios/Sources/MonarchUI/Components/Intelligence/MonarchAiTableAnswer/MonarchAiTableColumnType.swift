/// How a column of ``MonarchAiTableAnswer`` compares, aligns and formats its values.
public enum MonarchAiTableColumnType: Sendable, Hashable, CaseIterable {
    /// Text, compared in the reader's language with numbers in text compared by value.
    case text
    /// Numbers and numeric text such as "1,200", "-3.5" or "12%": aligned to the trailing edge and compared by value.
    case number
    /// ISO 8601 dates such as "2024-10-14" or "2024-10-14T09:30:00Z", compared by time.
    case date
}
