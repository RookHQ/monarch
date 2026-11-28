package dev.monarchui.components.collaboration.richtexteditor

import dev.monarchui.components.files.editortoolbar.MonarchEditorTool

/** The default tools and the limit check of `MonarchRichTextEditor`. */
public object MonarchRichTextEditorDefaults {
    /**
     * Undo and redo, the text style menu (body text and headings 1 to 3),
     * bold, italic, strikethrough, code and link, and both lists.
     */
    public val Tools: List<List<MonarchEditorTool>> by lazy {
        listOf(
            listOf(MonarchEditorTool.Undo, MonarchEditorTool.Redo),
            listOf(
                MonarchEditorTool.Paragraph,
                MonarchEditorTool.heading(1),
                MonarchEditorTool.heading(2),
                MonarchEditorTool.heading(3),
            ),
            listOf(
                MonarchEditorTool.Bold,
                MonarchEditorTool.Italic,
                MonarchEditorTool.Strikethrough,
                MonarchEditorTool.Code,
                MonarchEditorTool.Link,
            ),
            listOf(MonarchEditorTool.BulletList, MonarchEditorTool.OrderedList),
        )
    }

    /** Whether [text] has more than [maxLength] characters; `false` without a limit. */
    public fun isOverLimit(text: String, maxLength: Int?): Boolean = maxLength != null && text.length > maxLength
}
