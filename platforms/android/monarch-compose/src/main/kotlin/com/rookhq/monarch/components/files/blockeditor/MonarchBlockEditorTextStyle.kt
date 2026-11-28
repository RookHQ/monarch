package com.rookhq.monarch.components.files.blockeditor

/** The style of a text block of a `MonarchBlockEditor`. */
public enum class MonarchBlockEditorTextStyle(
    /** The block type of this style. */
    public val type: MonarchBlockEditorBlockType,
) {
    /** A paragraph of body text. */
    Paragraph(MonarchBlockEditorBlockType.Paragraph),

    /** A heading of level 1. */
    Heading1(MonarchBlockEditorBlockType.Heading1),

    /** A heading of level 2. */
    Heading2(MonarchBlockEditorBlockType.Heading2),

    /** A heading of level 3. */
    Heading3(MonarchBlockEditorBlockType.Heading3),

    /** An item of a bulleted list. Return continues the list; Return on an empty item ends it. */
    BulletedList(MonarchBlockEditorBlockType.BulletedList),

    /** An item of a numbered list, numbered with the items right before it. */
    NumberedList(MonarchBlockEditorBlockType.NumberedList),

    /** An item of a checklist, with a checkbox. */
    Checklist(MonarchBlockEditorBlockType.Checklist),

    /** A quotation. */
    Quote(MonarchBlockEditorBlockType.Quote),

    /** Monospaced code. Return adds a line instead of a block, and there is no inline formatting. */
    Code(MonarchBlockEditorBlockType.Code),
    ;

    /** Whether Return continues this style in the next block. */
    public val isList: Boolean get() = this == BulletedList || this == NumberedList || this == Checklist

    /** Whether this style is a heading. */
    public val isHeading: Boolean get() = this == Heading1 || this == Heading2 || this == Heading3

    /** Finds the text style of a block type. */
    public companion object {
        /** The text style of [type], or `null` for a block type that is not text. */
        public fun of(type: MonarchBlockEditorBlockType): MonarchBlockEditorTextStyle? =
            entries.firstOrNull { it.type == type }
    }
}
