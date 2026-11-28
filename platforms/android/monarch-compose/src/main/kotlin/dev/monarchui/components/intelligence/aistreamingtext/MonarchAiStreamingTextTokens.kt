package dev.monarchui.components.intelligence.aistreamingtext

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.components.intelligence.aimarkdown.MonarchAiMarkdownBlock
import dev.monarchui.components.intelligence.aimarkdown.MonarchAiMarkdownParser
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** Component tokens and the pure logic of MonarchAiStreamingText. */
internal object MonarchAiStreamingTextTokens {
    val CaretWidth: Dp = MonarchTokens.Space.space0_5.dp
    val CaretHeight: Dp = MonarchTokens.Space.space4.dp
    val CaretGap: Dp = MonarchTokens.Space.space1.dp

    /** The caret: the solid AI colour, as on the web. */
    fun caretColor(colors: MonarchColors): Color = colors.aiSolid

    /** Footnotes are numbered across the whole answer, so an answer with any is rendered as one document. */
    fun rendersWhole(text: String): Boolean = text.contains("[^")

    /** The source of each block: from its first line to the next block's first line. */
    fun sources(text: String, offsets: List<Int>): List<String> = offsets.mapIndexed { index, start ->
        text.substring(start, offsets.getOrNull(index + 1) ?: text.length)
    }

    /** What TalkBack hears for a block: its text, with code blocks and tables named instead of read out. */
    fun spoken(block: MonarchAiMarkdownBlock, codeBlock: String, table: String): String = when (block) {
        is MonarchAiMarkdownBlock.Code -> codeBlock
        is MonarchAiMarkdownBlock.Table -> table
        is MonarchAiMarkdownBlock.FootnoteDefinition -> ""
        else -> MonarchAiMarkdownParser.blockText(block)
    }.trim()

    /** The display source: finished blocks as they are, the last one repaired while [streaming]. */
    fun display(sources: List<String>, blocks: List<MonarchAiMarkdownBlock>, streaming: Boolean): List<String> =
        sources.mapIndexed { index, source ->
            val last = index == sources.lastIndex
            if (streaming && last && blocks.getOrNull(index) !is MonarchAiMarkdownBlock.Code) {
                MonarchAiStreamingTextRepair.complete(source)
            } else {
                source
            }
        }
}
