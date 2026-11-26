package com.rookhq.monarch.components.intelligence.aistructuredanswer

/** How a field of a `MonarchAiStructuredAnswer` shows its value. */
public enum class MonarchAiStructuredFieldType {
    /** Plain text. */
    Text,

    /** An array as a bulleted list; a single value is a list of one. */
    List,

    /** A number in the answer's locale, or in the field's own format. */
    Number,

    /** One `MonarchBadge` per value. */
    Badge,

    /** Markdown, rendered by `MonarchAiMarkdown` (never as HTML). */
    Markdown,
}
