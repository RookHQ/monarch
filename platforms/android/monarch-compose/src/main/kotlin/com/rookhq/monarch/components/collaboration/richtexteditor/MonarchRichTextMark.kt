package com.rookhq.monarch.components.collaboration.richtexteditor

/** A format of a `MonarchRichTextSpan`: an inline format, a link, or the heading style of a paragraph. */
public enum class MonarchRichTextMark {
    /** Bold text. */
    Bold,

    /** Italic text. */
    Italic,

    /** Struck-through text. */
    Strikethrough,

    /** Inline code, in the code typeface on the muted background. */
    Code,

    /** A link; the span's `url` is its address. Typing after a link does not extend it. */
    Link,

    /** A level 1 heading. Covers whole paragraphs. */
    Heading1,

    /** A level 2 heading. Covers whole paragraphs. */
    Heading2,

    /** A level 3 heading. Covers whole paragraphs. */
    Heading3,
    ;

    /** Whether this is a paragraph style, a heading, rather than an inline format. */
    public val isHeading: Boolean get() = this == Heading1 || this == Heading2 || this == Heading3
}
