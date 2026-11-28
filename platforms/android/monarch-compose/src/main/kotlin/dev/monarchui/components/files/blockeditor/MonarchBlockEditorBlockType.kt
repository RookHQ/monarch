package dev.monarchui.components.files.blockeditor

/**
 * The kinds of block a `MonarchBlockEditor` can hold and offer in its /
 * menu, its toolbar and its "Turn into" menu.
 */
public enum class MonarchBlockEditorBlockType {
    /** A paragraph of body text. */
    Paragraph,

    /** A heading of level 1, the page title. */
    Heading1,

    /** A heading of level 2, a section heading. */
    Heading2,

    /** A heading of level 3, a subsection heading. */
    Heading3,

    /** An item of a bulleted list. */
    BulletedList,

    /** An item of a numbered list. */
    NumberedList,

    /** An item of a checklist, with a checkbox. */
    Checklist,

    /** A quotation. */
    Quote,

    /** Monospaced code. */
    Code,

    /** A highlighted note with a tone, a `MonarchCalloutBlock`. */
    Callout,

    /** A summary that shows or hides its content. */
    Toggle,

    /** Rows and columns, a `MonarchTableBlock`. */
    Table,

    /** A video, design or demo from a link, a `MonarchEmbedBlock`. */
    Embed,
}
