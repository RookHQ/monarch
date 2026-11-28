package com.rookhq.monarch.components.intelligence.aimarkdown

import com.rookhq.monarch.components.intelligence.aimarkdown.MonarchAiMarkdownBlock as Block
import com.rookhq.monarch.components.intelligence.aimarkdown.MonarchAiMarkdownInline as Inline

/** Footnote numbers by label, and the definitions in the order they are listed at the end. */
internal class MonarchAiMarkdownFootnotes(
    val numbers: Map<String, Int>,
    val ordered: List<Pair<Int, Block.FootnoteDefinition>>,
) {
    companion object {
        /** Footnote labels referenced in [block], in reading order. */
        fun references(block: Block): List<String> = ArrayList<String>().also { walk(block, it) }

        private fun walk(block: Block, out: MutableList<String>) {
            when (block) {
                is Block.Heading -> inline(block.content, out)
                is Block.Paragraph -> inline(block.content, out)
                is Block.Table -> (listOf(block.header) + block.rows).forEach { row -> row.forEach { inline(it, out) } }
                is Block.Quote -> block.children.forEach { walk(it, out) }
                is Block.ListBlock -> block.items.forEach { item -> item.children.forEach { walk(it, out) } }
                else -> Unit
            }
        }

        private fun inline(nodes: List<Inline>, out: MutableList<String>) {
            for (n in nodes) {
                when (n) {
                    is Inline.FootnoteRef -> out += n.label
                    is Inline.Styled -> inline(n.children, out)
                    is Inline.Link -> inline(n.children, out)
                    else -> Unit
                }
            }
        }

        /**
         * Numbers footnotes in the order of their first reference, as the web
         * does. A definition that is never referenced is not listed; a reference
         * without a definition stays text.
         */
        fun of(blocks: List<Block>): MonarchAiMarkdownFootnotes {
            val definitions = LinkedHashMap<String, Block.FootnoteDefinition>()
            blocks.filterIsInstance<Block.FootnoteDefinition>().forEach { definitions.putIfAbsent(it.label, it) }
            val numbers = LinkedHashMap<String, Int>()
            blocks.forEach { block ->
                references(block).forEach { label ->
                    if (label in definitions && label !in numbers) numbers[label] = numbers.size + 1
                }
            }
            val ordered = numbers.map { (label, n) -> n to checkNotNull(definitions[label]) }
            return MonarchAiMarkdownFootnotes(numbers, ordered)
        }
    }
}
