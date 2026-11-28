package dev.monarchui.components.files.blockeditor

import androidx.compose.runtime.Immutable
import dev.monarchui.components.collaboration.richtexteditor.MonarchRichTextSpan
import dev.monarchui.components.files.tableblock.MonarchTableBlockData
import dev.monarchui.foundation.MonarchTone

/**
 * One block of a `MonarchBlockEditor` document. A document is a list of
 * blocks; save it from `MonarchBlockEditorState.blocks` and load it with
 * `MonarchBlockEditorState.setBlocks`. Text is plain text plus
 * `MonarchRichTextSpan`s for inline formats (bold, italic, strikethrough,
 * code, link), as in `MonarchRichTextEditorState`.
 */
@Immutable
public sealed interface MonarchBlockEditorBlock {
    /** A stable id, unique in the document. New blocks get one from `MonarchBlockEditorState.newId`. */
    public val id: String

    /** The kind of block. */
    public val type: MonarchBlockEditorBlockType

    /**
     * A block of text: a paragraph, a heading, a list item, a checklist item, a quote or code.
     *
     * @property id a stable id, unique in the document.
     * @property style the text style.
     * @property text the plain text. A line break is kept only in code; elsewhere Return starts a new block.
     * @property spans the inline formats of [text].
     * @property checked whether a checklist item is ticked. Ignored for other styles.
     */
    @Immutable
    public data class Text(
        override val id: String,
        public val style: MonarchBlockEditorTextStyle = MonarchBlockEditorTextStyle.Paragraph,
        public val text: String = "",
        public val spans: List<MonarchRichTextSpan> = emptyList(),
        public val checked: Boolean = false,
    ) : MonarchBlockEditorBlock {
        override val type: MonarchBlockEditorBlockType get() = style.type
    }

    /**
     * A highlighted note, drawn as a `MonarchCalloutBlock`.
     *
     * @property id a stable id, unique in the document.
     * @property text the note. Line breaks are kept.
     * @property spans the inline formats of [text].
     * @property tone the meaning, which selects the tint, the border and the spoken name.
     */
    @Immutable
    public data class Callout(
        override val id: String,
        public val text: String = "",
        public val spans: List<MonarchRichTextSpan> = emptyList(),
        public val tone: MonarchTone = MonarchTone.Info,
    ) : MonarchBlockEditorBlock {
        override val type: MonarchBlockEditorBlockType get() = MonarchBlockEditorBlockType.Callout
    }

    /**
     * A summary that shows or hides its content, drawn as a `MonarchToggleBlock` when read.
     *
     * @property id a stable id, unique in the document.
     * @property text the summary, on one line.
     * @property spans the inline formats of the summary.
     * @property body the content shown while expanded, as plain text with line breaks.
     * @property expanded whether the content is shown.
     */
    @Immutable
    public data class Toggle(
        override val id: String,
        public val text: String = "",
        public val spans: List<MonarchRichTextSpan> = emptyList(),
        public val body: String = "",
        public val expanded: Boolean = true,
    ) : MonarchBlockEditorBlock {
        override val type: MonarchBlockEditorBlockType get() = MonarchBlockEditorBlockType.Toggle
    }

    /**
     * Rows and columns, edited in place as a `MonarchTableBlock`.
     *
     * @property id a stable id, unique in the document.
     * @property data the cells and the header row.
     */
    @Immutable
    public data class Table(
        override val id: String,
        public val data: MonarchTableBlockData = MonarchTableBlockData.empty(),
    ) : MonarchBlockEditorBlock {
        override val type: MonarchBlockEditorBlockType get() = MonarchBlockEditorBlockType.Table
    }

    /**
     * A video, design or demo from a link, drawn as a `MonarchEmbedBlock`. While [url] is empty the editor
     * asks for the link.
     *
     * @property id a stable id, unique in the document.
     * @property url the address of the page to embed, or an empty string while it is not known.
     * @property caption a caption under the embed.
     */
    @Immutable
    public data class Embed(
        override val id: String,
        public val url: String = "",
        public val caption: String? = null,
    ) : MonarchBlockEditorBlock {
        override val type: MonarchBlockEditorBlockType get() = MonarchBlockEditorBlockType.Embed
    }
}
