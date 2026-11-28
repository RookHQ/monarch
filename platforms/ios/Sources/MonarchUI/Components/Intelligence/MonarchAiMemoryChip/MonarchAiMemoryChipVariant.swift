/// How a ``MonarchAiMemoryChip`` is drawn.
public enum MonarchAiMemoryChipVariant: Sendable, Hashable, CaseIterable {
    /// An inline pill with the AI glyph and a prefix such as "Remembered:",
    /// for a conversation thread.
    case chip
    /// A full-width row without the prefix, for a list of memories.
    case row
}
