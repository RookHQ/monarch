/// How a field of ``MonarchAiStructuredAnswer`` shows its value.
public enum MonarchAiStructuredFieldType: Sendable, Hashable, CaseIterable {
    /// Plain text.
    case text
    /// An array as a bulleted list. Spans the full width by default.
    case list
    /// A number, written in the reader's locale with the field's format.
    case number
    /// One ``MonarchBadge`` per value.
    case badge
    /// Markdown, rendered by ``MonarchAiMarkdown``. Spans the full width by default.
    case markdown
}
