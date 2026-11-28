package com.rookhq.monarch.components.typography.prosestyle

import androidx.compose.ui.text.AnnotatedString

/**
 * The blocks of a `MonarchProse`, added in reading order. Running text is an
 * [AnnotatedString], so links (`LinkAnnotation` with
 * `MonarchProseStyle.linkStyles()`), bold, italic and inline code
 * (`MonarchInlineCodeStyle`) are styled runs inside it.
 */
@Suppress("TooManyFunctions")
public interface MonarchProseScope {
    /**
     * A heading.
     *
     * @param text the heading text.
     * @param level 1 to 6, as in HTML; values outside are clamped. Level 1 is the largest.
     */
    public fun heading(text: AnnotatedString, level: Int = 2)

    /** A heading of plain text. See the [AnnotatedString] overload. */
    public fun heading(text: String, level: Int = 2): Unit = heading(AnnotatedString(text), level)

    /** A paragraph of running text. */
    public fun paragraph(text: AnnotatedString)

    /** A paragraph of plain text. */
    public fun paragraph(text: String): Unit = paragraph(AnnotatedString(text))

    /**
     * An item of a list.
     *
     * @param text the item's text.
     * @param marker the item's number or letter as written, such as "2." or "b)"; read by TalkBack. `null` draws
     * a bullet, which TalkBack skips.
     * @param depth how deeply the list is nested, from 0. Each level indents by 1.5 of the base size.
     */
    public fun listItem(text: AnnotatedString, marker: String? = null, depth: Int = 0)

    /** An item of a list, of plain text. See the [AnnotatedString] overload. */
    public fun listItem(text: String, marker: String? = null, depth: Int = 0): Unit =
        listItem(AnnotatedString(text), marker, depth)

    /** A quotation, with a bar at the start edge and the muted text colour. Wrap plain text in [AnnotatedString]. */
    public fun quote(text: AnnotatedString)

    /** A block of code, in the code typeface on a panel; long lines scroll sideways and read left to right. */
    public fun code(text: String)

    /** A caption under a figure or a table, in the muted colour. */
    public fun caption(text: String)

    /**
     * A table of text: a header row of [columns], then a row of cells per entry of [rows], with a line under each.
     * Cells wrap inside equal columns.
     */
    public fun table(columns: List<String>, rows: List<List<String>>) {}

    /** A thematic break: a line between sections, hidden from TalkBack. */
    public fun rule()
}
