package dev.monarchui.components.intelligence.aimarkdown

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.R
import dev.monarchui.components.forms.checkbox.MonarchCheckbox
import dev.monarchui.components.intelligence.aimarkdown.MonarchAiMarkdownBlock as Block
import dev.monarchui.components.layout.divider.MonarchDivider
import dev.monarchui.components.layout.divider.MonarchDividerVariant
import dev.monarchui.components.navigation.link.MonarchLinkDefaults
import dev.monarchui.components.typography.codeblock.MonarchCodeBlock
import dev.monarchui.foundation.MonarchTypography
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme

/**
 * Renders GitHub-flavoured Markdown written by a model as Compose text and
 * layouts, never as HTML: headings, paragraphs with emphasis, strikethrough,
 * code spans and links, ordered, unordered, nested and task lists,
 * quotations, fenced code, tables, rules and footnotes.
 *
 * Built by Monarch: neither Compose nor Material 3 renders block Markdown.
 * The parser is a port of the web's, so both platforms read the same answer
 * the same way, and it is incremental: while [text] grows during streaming,
 * only the last block is parsed again. The source is untrusted: raw HTML stays
 * text, a link opens only for `http`, `https` or `mailto` (or a relative link
 * when [onOpenLink] handles it) and keeps its text otherwise, links show their
 * host when the text does not name it, and images show their alt text unless
 * the app renders them through [image]. Fenced code renders in
 * `MonarchCodeBlock`, or in [codeBlock].
 *
 * Each block is its own TalkBack element; headings have heading semantics,
 * task items read "Task" with their checked state, and tables give their
 * size and each cell's position. The column does not scroll: put it in the
 * scrolling message list.
 *
 * @param text the Markdown source, treated as untrusted.
 * @param modifier the modifier of the column.
 * @param streaming the text is still arriving: an unclosed code block is incomplete, so its copy button is
 * hidden and [codeBlock] receives `complete = false`.
 * @param showLinkHosts shows the host after a link whose text does not name it, such as "Read more (example.com)".
 * @param onOpenLink opens a link instead of the system browser. When given, relative links are links too.
 * @param image renders a paragraph that is only an image with an `http` or `https` source and alt text. Monarch
 * loads no images; without it the alt text is shown.
 * @param codeBlock renders every code block instead of `MonarchCodeBlock`, for example a diagram for `mermaid`
 * or typeset maths for `math` (display maths written as `$$` or `\[` on their own line).
 * @param labels replacements for the built-in strings.
 */
@Composable
public fun MonarchAiMarkdown(
    text: String,
    modifier: Modifier = Modifier,
    streaming: Boolean = false,
    showLinkHosts: Boolean = true,
    onOpenLink: ((url: String) -> Unit)? = null,
    image: (@Composable (url: String, alt: String) -> Unit)? = null,
    codeBlock: (@Composable (language: String, code: String, complete: Boolean) -> Unit)? = null,
    labels: MonarchAiMarkdownLabels = MonarchAiMarkdownLabels(),
) {
    val cache = remember { arrayOfNulls<MonarchAiMarkdownParseState>(1) }
    val parsed = remember(text) { MonarchAiMarkdownParser.parseIncremental(text, cache[0]).also { cache[0] = it } }
    val blocks = parsed.blocks
    val footnotes = remember(blocks) { MonarchAiMarkdownFootnotes.of(blocks) }
    val theme = MonarchTheme.colors
    val typography = MonarchTheme.typography
    val linkStyles = MonarchLinkDefaults.styles()
    val resources = LocalResources.current
    val linkHost = labels.linkHost
        ?: { host: String -> resources.getString(R.string.monarch_aimarkdown_link_host, host) }
    val annotator = MonarchAiMarkdownAnnotator(
        numbers = footnotes.numbers,
        styles = MonarchAiMarkdownTokens.spanStyles(theme, typography, linkStyles),
        showLinkHosts = showLinkHosts,
        linkHost = linkHost,
        onOpenLink = onOpenLink,
    )
    val env = MarkdownEnv(
        annotate = annotator::annotate,
        colors = MonarchAiMarkdownTokens.colors(theme),
        typography = typography,
        streaming = streaming,
        tableLabel = monarchString(labels.table, R.string.monarch_aimarkdown_table),
        taskLabel = monarchString(labels.task, R.string.monarch_aimarkdown_task),
        image = image,
        codeBlock = codeBlock,
    )
    Column(modifier = modifier) {
        Blocks(blocks, env, depth = 0, tight = false, muted = false)
        if (footnotes.ordered.isNotEmpty()) {
            Footnotes(footnotes, env, monarchString(labels.footnotes, R.string.monarch_aimarkdown_footnotes))
        }
    }
}

private class MarkdownEnv(
    val annotate: (List<MonarchAiMarkdownInline>) -> AnnotatedString,
    val colors: MonarchAiMarkdownColors,
    val typography: MonarchTypography,
    val streaming: Boolean,
    val tableLabel: String,
    val taskLabel: String,
    val image: (@Composable (url: String, alt: String) -> Unit)?,
    val codeBlock: (@Composable (language: String, code: String, complete: Boolean) -> Unit)?,
) {
    fun body(muted: Boolean): TextStyle =
        MonarchAiMarkdownTokens.body(typography, if (muted) colors.quoteText else colors.text)
}

@Composable
private fun Blocks(blocks: List<Block>, env: MarkdownEnv, depth: Int, tight: Boolean, muted: Boolean) {
    var previous: Block? = null
    for (block in blocks) {
        if (block is Block.FootnoteDefinition) continue
        val gap = MonarchAiMarkdownTokens.gapBefore(block, previous, tight)
        BlockView(block, env, depth, muted, Modifier.padding(top = gap))
        previous = block
    }
}

@Composable
private fun BlockView(block: Block, env: MarkdownEnv, depth: Int, muted: Boolean, modifier: Modifier) {
    val colors = env.colors
    when (block) {
        is Block.Heading -> Text(
            text = env.annotate(block.content),
            modifier = modifier.semantics { heading() },
            style = MonarchAiMarkdownTokens.heading(
                block.level,
                env.typography,
                if (muted) colors.quoteText else colors.text,
            ),
        )
        is Block.Paragraph -> {
            val picture = MonarchAiMarkdownTokens.standaloneImage(block)
            if (picture != null && env.image != null) {
                Box(modifier = modifier) { env.image.invoke(picture.src, picture.alt) }
            } else {
                Text(text = env.annotate(block.content), modifier = modifier, style = env.body(muted))
            }
        }
        is Block.Code -> CodeView(block, env, modifier)
        is Block.Quote -> Column(
            modifier = modifier
                .drawBehind {
                    val width = MonarchAiMarkdownTokens.QuoteBarWidth.toPx()
                    val x = if (layoutDirection == LayoutDirection.Rtl) size.width - width else 0f
                    drawRect(colors.quoteBar, Offset(x, 0f), Size(width, size.height))
                }
                .padding(start = MonarchAiMarkdownTokens.QuoteIndent),
        ) {
            Blocks(block.children, env, depth, tight = false, muted = true)
        }
        is Block.ListBlock -> ListView(block, env, depth, muted, modifier)
        is Block.Table -> MonarchAiMarkdownTable(
            table = block,
            annotate = env.annotate,
            body = env.body(muted),
            colors = colors,
            label = env.tableLabel,
            modifier = modifier,
        )
        is Block.Rule -> MonarchDivider(
            modifier = modifier.clearAndSetSemantics {},
            variant = MonarchDividerVariant.Subtle,
        )
        is Block.FootnoteDefinition -> Unit
    }
}

@Composable
private fun CodeView(block: Block.Code, env: MarkdownEnv, modifier: Modifier) {
    val complete = block.closed || !env.streaming
    Box(modifier = modifier) {
        if (env.codeBlock != null) {
            env.codeBlock.invoke(block.lang, block.code, complete)
        } else {
            MonarchCodeBlock(
                code = block.code,
                language = block.lang.ifEmpty { null },
                title = block.filename,
                copyable = complete,
            )
        }
    }
}

@Composable
private fun ListView(list: Block.ListBlock, env: MarkdownEnv, depth: Int, muted: Boolean, modifier: Modifier) {
    val tokens = MonarchAiMarkdownTokens
    val gap = if (list.loose) tokens.BlockGap else tokens.TightGap
    Column(modifier = modifier) {
        list.items.forEachIndexed { index, item ->
            Row(modifier = if (index > 0) Modifier.padding(top = gap) else Modifier) {
                Box(modifier = Modifier.widthIn(min = tokens.MarkerWidth).padding(end = tokens.MarkerGap)) {
                    Marker(list, index, item, env, depth, muted)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Blocks(item.children, env, depth + 1, tight = !list.loose, muted = muted)
                }
            }
        }
    }
}

@Composable
private fun Marker(
    list: Block.ListBlock,
    index: Int,
    item: MonarchAiMarkdownListItem,
    env: MarkdownEnv,
    depth: Int,
    muted: Boolean,
) {
    val style = env.body(muted).copy(color = env.colors.marker)
    val checked = item.checked
    when {
        checked != null -> MonarchCheckbox(
            checked = checked,
            onCheckedChange = null,
            // A task mark in an answer is read-only: the quiet disabled mark, as on the web.
            enabled = false,
            modifier = Modifier.semantics {
                role = Role.Checkbox
                contentDescription = env.taskLabel
                toggleableState = ToggleableState(checked)
            },
        )
        // A number is read before the item; a bullet is decoration.
        list.ordered -> Text(text = "${list.start + index}.", style = style)
        else -> Text(
            text = MonarchAiMarkdownTokens.bullet(depth),
            modifier = Modifier.clearAndSetSemantics {},
            style = style,
        )
    }
}

@Composable
private fun Footnotes(footnotes: MonarchAiMarkdownFootnotes, env: MarkdownEnv, label: String) {
    val tokens = MonarchAiMarkdownTokens
    val style = env.body(muted = false).copy(color = env.colors.marker)
    Column(modifier = Modifier.padding(top = tokens.HeadingGap)) {
        MonarchDivider(
            modifier = Modifier.clearAndSetSemantics {},
            variant = MonarchDividerVariant.Subtle,
        )
        // As on the web, the heading is for assistive technology only: the divider and the notes say it.
        Box(modifier = Modifier.padding(top = tokens.BlockGap).semantics {
            heading()
            contentDescription = label
        })
        footnotes.ordered.forEach { (number, definition) ->
            Row(modifier = Modifier.padding(top = tokens.TightGap)) {
                Box(modifier = Modifier.widthIn(min = tokens.MarkerWidth).padding(end = tokens.MarkerGap)) {
                    Text(text = "$number.", style = style)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Blocks(definition.children, env, depth = 0, tight = true, muted = false)
                }
            }
        }
    }
}
