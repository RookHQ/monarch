package com.rookhq.monarch.components.files.blockeditor

import com.rookhq.monarch.components.files.editortoolbar.MonarchEditorTool

/** The default tools of `MonarchBlockEditor`. */
public object MonarchBlockEditorDefaults {
    /**
     * Undo and redo, the text style menu (body text and headings 1 to 3),
     * bold, italic, strikethrough, code and link, then the lists, quote and
     * code block, which turn the block with the caret into that kind.
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
            listOf(
                MonarchEditorTool.BulletList,
                MonarchEditorTool.OrderedList,
                MonarchEditorTool.TaskList,
                MonarchEditorTool.Blockquote,
                MonarchEditorTool.CodeBlock,
            ),
        )
    }
}
