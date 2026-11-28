package dev.monarchui.components.intelligence.aistreamingtext

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import dev.monarchui.R
import dev.monarchui.components.intelligence.aimarkdown.MonarchAiMarkdown
import dev.monarchui.components.intelligence.aimarkdown.MonarchAiMarkdownBlock
import dev.monarchui.components.intelligence.aimarkdown.MonarchAiMarkdownLabels
import dev.monarchui.components.intelligence.aimarkdown.MonarchAiMarkdownParseState
import dev.monarchui.components.intelligence.aimarkdown.MonarchAiMarkdownParser
import dev.monarchui.components.intelligence.aimarkdown.MonarchAiMarkdownTokens
import dev.monarchui.internal.MonarchLiveStatus
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme
import kotlinx.coroutines.delay
import java.util.IdentityHashMap

/**
 * Renders a Markdown answer while it streams: finished blocks stay put,
 * half-written syntax never flashes, and TalkBack hears whole sentences, not
 * tokens.
 *
 * Built by Monarch on `MonarchAiMarkdown`; Compose has no streaming text. The
 * answer is parsed incrementally, so every block before the last keeps its
 * identity, and each block is its own keyed `MonarchAiMarkdown`: a finished
 * block is skipped by recomposition while tokens arrive, and only the last
 * block is parsed and drawn again. While [streaming], the last block has its
 * open bold, italics, strikethrough and code spans closed for display, a link
 * shows only its text until its target arrives, and table rows wait for the
 * delimiter row; an unclosed code fence is already a code block. An answer
 * with footnotes is drawn as one document, so their numbers stay right.
 *
 * Unless [announces] is off, a polite live region announces complete
 * sentences and paragraphs at most once a second, with code blocks and tables
 * named rather than read, then "Response complete". Text that was never
 * streamed here, such as history, is not announced. A still caret in the
 * solid AI colour follows the text while it streams; it is decorative.
 *
 * @param text the Markdown received so far. It normally only grows while [streaming].
 * @param streaming whether more text is arriving.
 * @param modifier the modifier of the column.
 * @param announces whether new sentences and the end are announced. Turn it off when the thread announces
 * messages itself.
 * @param announceComplete whether "Response complete" may be announced when this text stops streaming. Pass
 * `false` while the reply as a whole goes on (a tool runs after this text), then `true` once it finishes.
 * @param showCaret shows the caret after the text while [streaming].
 * @param showLinkHosts passed to `MonarchAiMarkdown`.
 * @param onOpenLink passed to `MonarchAiMarkdown`.
 * @param image passed to `MonarchAiMarkdown`.
 * @param codeBlock passed to `MonarchAiMarkdown`; `complete` is `false` for a code block still arriving.
 * @param labels replacements for the spoken strings.
 * @param markdownLabels passed to `MonarchAiMarkdown` as its labels.
 */
@Composable
public fun MonarchAiStreamingText(
    text: String,
    streaming: Boolean,
    modifier: Modifier = Modifier,
    announces: Boolean = true,
    announceComplete: Boolean = true,
    showCaret: Boolean = true,
    showLinkHosts: Boolean = true,
    onOpenLink: ((url: String) -> Unit)? = null,
    image: (@Composable (url: String, alt: String) -> Unit)? = null,
    codeBlock: (@Composable (language: String, code: String, complete: Boolean) -> Unit)? = null,
    labels: MonarchAiStreamingTextLabels = MonarchAiStreamingTextLabels(),
    markdownLabels: MonarchAiMarkdownLabels = MonarchAiMarkdownLabels(),
) {
    val tokens = MonarchAiStreamingTextTokens
    val cache = remember { arrayOfNulls<MonarchAiMarkdownParseState>(1) }
    val parsed = remember(text) { MonarchAiMarkdownParser.parseIncremental(text, cache[0]).also { cache[0] = it } }
    val display = remember(parsed, streaming) {
        tokens.display(tokens.sources(parsed.text, parsed.offsets), parsed.blocks, streaming)
    }
    // Equal labels keep one instance, so finished blocks are skipped while tokens arrive.
    val stableLabels = remember(
        markdownLabels.table,
        markdownLabels.footnotes,
        markdownLabels.task,
        markdownLabels.linkHost,
    ) { markdownLabels }
    val options = BlockOptions(showLinkHosts, onOpenLink, image, codeBlock, stableLabels)
    Column(modifier = modifier) {
        val first = parsed.blocks.firstOrNull()
        if (first != null && tokens.rendersWhole(text)) {
            Block(display.joinToString(""), MonarchAiMarkdownTokens.gapBefore(first, null, false), streaming, options)
        } else {
            parsed.blocks.forEachIndexed { index, block ->
                val gap = MonarchAiMarkdownTokens.gapBefore(block, parsed.blocks.getOrNull(index - 1), false)
                key(parsed.offsets[index]) {
                    Block(display[index], gap, streaming && index == parsed.blocks.lastIndex, options)
                }
            }
        }
        if (streaming && showCaret) {
            Box(
                Modifier
                    .padding(top = tokens.CaretGap)
                    .size(tokens.CaretWidth, tokens.CaretHeight)
                    .background(tokens.caretColor(MonarchTheme.colors))
                    .clearAndSetSemantics {},
            )
        }
        if (announces) {
            val live = rememberStreamingAnnouncement(parsed.blocks, display, streaming, announceComplete, labels)
            // Always present, so each new sentence is announced as a change of this live region.
            MonarchLiveStatus(text = live.takeIf { it.isNotEmpty() }, visible = false)
        }
    }
}

/** The options every block is drawn with; one value, so a finished block's arguments stay equal. */
private data class BlockOptions(
    val showLinkHosts: Boolean,
    val onOpenLink: ((url: String) -> Unit)?,
    val image: (@Composable (url: String, alt: String) -> Unit)?,
    val codeBlock: (@Composable (language: String, code: String, complete: Boolean) -> Unit)?,
    val labels: MonarchAiMarkdownLabels,
)

@Composable
private fun Block(source: String, gap: Dp, streaming: Boolean, options: BlockOptions) {
    MonarchAiMarkdown(
        text = source,
        modifier = Modifier.padding(top = gap),
        streaming = streaming,
        showLinkHosts = options.showLinkHosts,
        onOpenLink = options.onOpenLink,
        image = options.image,
        codeBlock = options.codeBlock,
        labels = options.labels,
    )
}

/** The latest announcement; empty until a sentence is complete. */
@Composable
private fun rememberStreamingAnnouncement(
    blocks: List<MonarchAiMarkdownBlock>,
    display: List<String>,
    streaming: Boolean,
    announceComplete: Boolean,
    labels: MonarchAiStreamingTextLabels,
): String {
    val tokens = MonarchAiStreamingTextTokens
    val codeBlock = monarchString(labels.codeBlock, R.string.monarch_aistreamingtext_code_block)
    val table = monarchString(labels.table, R.string.monarch_aistreamingtext_table)
    val complete = monarchString(labels.complete, R.string.monarch_aistreamingtext_complete)
    val cache = remember(codeBlock, table) { IdentityHashMap<MonarchAiMarkdownBlock, String>() }
    val finished = if (streaming) blocks.dropLast(1) else blocks
    val done = finished.map { block -> cache.getOrPut(block) { tokens.spoken(block, codeBlock, table) } }
        .filter { it.isNotEmpty() }
        .joinToString("\n\n")
    if (cache.size > finished.size) cache.keys.retainAll { kept -> finished.any { it === kept } }
    val tail = if (streaming && display.isNotEmpty()) {
        MonarchAiMarkdownParser.parse(display.last()).map { tokens.spoken(it, codeBlock, table) }
            .filter { it.isNotEmpty() }
            .joinToString("\n\n")
    } else {
        ""
    }
    val spoken = if (done.isNotEmpty() && tail.isNotEmpty()) "$done\n\n$tail" else done.ifEmpty { tail }
    val announcer = remember { MonarchAiStreamingTextAnnouncer() }
    var live by remember { mutableStateOf("") }
    LaunchedEffect(spoken, streaming, announceComplete, complete) {
        while (true) {
            val now = withFrameMillis { it }
            val step = announcer.step(spoken, done.length, streaming, announceComplete, now)
            step.say(complete)?.let { live = it }
            delay(step.retryAfterMillis ?: break)
        }
    }
    return live
}
