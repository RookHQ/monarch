package com.rookhq.monarch.components.collaboration.richtexteditor

import androidx.compose.runtime.Immutable

/**
 * One formatted range of a `MonarchRichTextEditorState` document, from
 * [start] (inclusive) to [end] (exclusive) in its text. A document is plain
 * text plus these spans, which an app can store as they are; lists are text
 * prefixes ("• ", "1. ") in the text itself.
 *
 * @property mark the format.
 * @property start the index of the first formatted character.
 * @property end the index after the last formatted character.
 * @property url the address of a [MonarchRichTextMark.Link]; `null` for other marks.
 */
@Immutable
public data class MonarchRichTextSpan(
    public val mark: MonarchRichTextMark,
    public val start: Int,
    public val end: Int,
    public val url: String? = null,
)
