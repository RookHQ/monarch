package com.rookhq.monarch.components.files.editortoolbar

import androidx.compose.runtime.Immutable

/** What a tool does in the toolbar: runs once, sets the text style, or turns a format on and off. */
internal enum class EditorToolbarToolKind { Action, TextStyle, Toggle }

/**
 * One tool of a `MonarchEditorToolbar`: a format, a text style, undo and
 * redo, or a product tool. Two tools are equal when their [id]s are.
 *
 * @property id identifies the tool, such as "bold", "heading-2" or "custom:mention".
 * @property level the heading level, 1 to 6, of a [heading] tool; `null` for every other tool.
 * @property title the visible name of a [custom] tool; `null` for the built-in tools, whose names are
 * built-in strings.
 */
@Immutable
public class MonarchEditorTool private constructor(
    public val id: String,
    public val level: Int?,
    public val title: String?,
    internal val kind: EditorToolbarToolKind,
) {
    override fun equals(other: Any?): Boolean = other is MonarchEditorTool && other.id == id

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "MonarchEditorTool($id)"

    /** The built-in tools, the factories and the default groups. */
    public companion object {
        private const val MIN_LEVEL = 1
        private const val MAX_LEVEL = 6

        private fun toggle(id: String) = MonarchEditorTool(id, null, null, EditorToolbarToolKind.Toggle)

        private fun action(id: String) = MonarchEditorTool(id, null, null, EditorToolbarToolKind.Action)

        /** Undoes the last change. A button. */
        public val Undo: MonarchEditorTool = action("undo")

        /** Redoes the last undone change. A button. */
        public val Redo: MonarchEditorTool = action("redo")

        /** Plain paragraph text. An entry of the text style menu. */
        public val Paragraph: MonarchEditorTool =
            MonarchEditorTool("paragraph", null, null, EditorToolbarToolKind.TextStyle)

        /** Bold. A toggle. */
        public val Bold: MonarchEditorTool = toggle("bold")

        /** Italic. A toggle. */
        public val Italic: MonarchEditorTool = toggle("italic")

        /** Strikethrough. A toggle. */
        public val Strikethrough: MonarchEditorTool = toggle("strikethrough")

        /** Inline code. A toggle. */
        public val Code: MonarchEditorTool = toggle("code")

        /** A link. A toggle that opens the link form when the toolbar has `onLink`. */
        public val Link: MonarchEditorTool = toggle("link")

        /** A bulleted list. A toggle. */
        public val BulletList: MonarchEditorTool = toggle("bullet-list")

        /** A numbered list. A toggle. */
        public val OrderedList: MonarchEditorTool = toggle("ordered-list")

        /** A task list. A toggle. */
        public val TaskList: MonarchEditorTool = toggle("task-list")

        /** A quote. A toggle. */
        public val Blockquote: MonarchEditorTool = toggle("blockquote")

        /** A code block. A toggle. */
        public val CodeBlock: MonarchEditorTool = toggle("code-block")

        /** A heading of [level], an entry of the text style menu. A level outside 1 to 6 becomes the nearest. */
        public fun heading(level: Int): MonarchEditorTool {
            val clamped = level.coerceIn(MIN_LEVEL, MAX_LEVEL)
            return MonarchEditorTool("heading-$clamped", clamped, null, EditorToolbarToolKind.TextStyle)
        }

        /** A product tool, such as Mention or Insert image: a button named [title] whose id is "custom:[id]". */
        public fun custom(id: String, title: String): MonarchEditorTool =
            MonarchEditorTool("custom:$id", null, title, EditorToolbarToolKind.Action)

        /** Undo and redo, the text style menu, inline formats, lists, and blocks. */
        public val DefaultGroups: List<List<MonarchEditorTool>> by lazy {
            listOf(
                listOf(Undo, Redo),
                listOf(Paragraph, heading(1), heading(2), heading(3)),
                listOf(Bold, Italic, Strikethrough, Code, Link),
                listOf(BulletList, OrderedList, TaskList),
                listOf(Blockquote, CodeBlock),
            )
        }

        /** Inline formats only, for a toolbar above the keyboard or a comment field. */
        public val InlineGroups: List<List<MonarchEditorTool>> by lazy {
            listOf(listOf(Bold, Italic, Strikethrough, Code, Link))
        }
    }
}
