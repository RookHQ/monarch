package com.rookhq.monarch.components.typography.codeblock

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.actions.copybutton.MonarchCopyButton
import com.rookhq.monarch.components.datadisplay.tag.MonarchChip
import com.rookhq.monarch.components.datadisplay.tag.MonarchTagSize
import com.rookhq.monarch.components.layout.divider.MonarchDivider
import com.rookhq.monarch.components.layout.divider.MonarchDividerVariant
import com.rookhq.monarch.internal.MonarchSurfaceColors
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.internal.monarchSurface
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.theme.MonarchTheme

/**
 * A code sample with a header (title or language), copy, a wrap toggle,
 * optional line numbers, light syntax colouring and a diff mode.
 *
 * Built by Monarch from monospaced `Text` with an `AnnotatedString` per line,
 * in a `horizontalScroll` region so long lines scroll inside the block and
 * never the screen. The wrap toggle is a small `MonarchChip` and the copy
 * button is `MonarchCopyButton`. The highlighter is ported from the web and
 * colours JavaScript and TypeScript, JSON, Python, shell, CSS and SCSS, HTML,
 * XML and SVG, SQL, Go and Rust; other languages are plain text. Code always
 * reads left to right; the header follows the layout direction.
 *
 * TalkBack reads the code region by its name (the title, else "TypeScript
 * code", else "Code") and then each line as one element. Line numbers and diff
 * markers are not read; added and removed lines start with "Added:" or
 * "Removed:"; lines with only white space are skipped.
 *
 * @param code the code, exactly as it is copied. Tabs are drawn as four spaces and copied as tabs.
 * @param modifier the modifier of the block.
 * @param language a language id ("ts", "python", "bash", "diff"). Picks the highlighter and the header text.
 * An unknown id is plain text and is shown as given.
 * @param title the header text, such as a file name. Also names the code. `null` shows the language name.
 * @param diff draws `+` and `-` lines as a diff. `null` means true when [language] is "diff" or "patch".
 * @param highlighted turns syntax colouring on or off.
 * @param showLineNumbers shows line numbers before the lines. They are not read and not copied.
 * @param showWrapToggle shows the "Wrap lines" toggle in the header.
 * @param copyable shows the copy button in the header.
 * @param wrapped whether long lines wrap instead of scrolling sideways. `null` lets the block keep this
 * itself, starting unwrapped.
 * @param onWrappedChange called with the new state when the user uses the wrap toggle.
 * @param onCopy called with the copied code after each copy.
 * @param labels replacements for the built-in strings.
 * @param headerActions extra controls in the header, before the wrap toggle and the copy button, such as a
 * "Run" button, a file-tab menu or a language picker. Each control keeps its own name and 48dp target.
 */
@Composable
@OptIn(ExperimentalLayoutApi::class)
public fun MonarchCodeBlock(
    code: String,
    modifier: Modifier = Modifier,
    language: String? = null,
    title: String? = null,
    diff: Boolean? = null,
    highlighted: Boolean = true,
    showLineNumbers: Boolean = false,
    showWrapToggle: Boolean = true,
    copyable: Boolean = true,
    wrapped: Boolean? = null,
    onWrappedChange: ((Boolean) -> Unit)? = null,
    onCopy: ((String) -> Unit)? = null,
    labels: MonarchCodeBlockLabels = MonarchCodeBlockLabels(),
    headerActions: (@Composable RowScope.() -> Unit)? = null,
) {
    val colors = MonarchTheme.colors
    val shape = MonarchTheme.shapes.md
    var ownWrapped by rememberSaveable { mutableStateOf(false) }
    val isWrapped = wrapped ?: ownWrapped
    val isDiff = MonarchCodeBlockLines.isDiff(diff, language)
    val lines = remember(code, language, isDiff, highlighted) {
        MonarchCodeBlockLines.lines(code, language, isDiff, highlighted)
    }
    val heading = title ?: MonarchCodeBlockHighlighter.languageName(language)
    val name = codeName(labels, title, language)
    val surface = MonarchSurfaceColors(
        container = MonarchCodeBlockTokens.container(colors),
        content = colors.textDefault,
        border = MonarchCodeBlockTokens.border(colors),
    )
    Column(modifier = modifier.monarchSurface(surface, shape).clip(shape)) {
        if (listOf(heading != null, showWrapToggle, copyable, headerActions != null).any { it }) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MonarchCodeBlockTokens.header(colors))
                    .padding(
                        start = MonarchCodeBlockTokens.HeaderPaddingStart,
                        end = MonarchCodeBlockTokens.HeaderPaddingEnd,
                        top = MonarchCodeBlockTokens.HeaderPaddingVertical,
                        bottom = MonarchCodeBlockTokens.HeaderPaddingVertical,
                    ),
                horizontalArrangement = Arrangement.spacedBy(MonarchCodeBlockTokens.HeaderGap, Alignment.End),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                if (heading != null) {
                    Text(
                        text = heading,
                        modifier = Modifier.weight(1f),
                        style = MonarchTheme.typography.code,
                        color = MonarchCodeBlockTokens.muted(colors),
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(MonarchCodeBlockTokens.HeaderGap),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    headerActions?.invoke(this)
                    if (showWrapToggle) {
                        MonarchChip(
                            text = monarchString(labels.wrap, R.string.monarch_codeblock_wrap),
                            selected = isWrapped,
                            onSelectedChange = { next ->
                                if (wrapped == null) ownWrapped = next
                                onWrappedChange?.invoke(next)
                            },
                            size = MonarchTagSize.Sm,
                        )
                    }
                    if (copyable) {
                        MonarchCopyButton(
                            value = code,
                            variant = MonarchButtonVariant.Ghost,
                            size = MonarchButtonSize.Sm,
                            target = name,
                            onCopy = onCopy,
                            label = labels.copy,
                            copiedLabel = labels.copied,
                            copiedAnnouncement = labels.copiedAnnouncement,
                        )
                    }
                }
            }
            MonarchDivider(variant = MonarchDividerVariant.Subtle)
        }
        CodeLines(
            lines = lines,
            name = name,
            diff = isDiff,
            wrapped = isWrapped,
            numbers = showLineNumbers,
            labels = labels,
        )
    }
}

@Composable
private fun codeName(labels: MonarchCodeBlockLabels, title: String?, language: String?): String {
    labels.codeName?.let { return it(title, language) }
    if (title != null) return title
    val languageName = MonarchCodeBlockHighlighter.languageName(language)
    return if (languageName != null) {
        stringResource(R.string.monarch_codeblock_language_code, languageName)
    } else {
        stringResource(R.string.monarch_codeblock_code)
    }
}

@Composable
@Suppress("LongParameterList")
private fun CodeLines(
    lines: List<MonarchCodeBlockLine>,
    name: String,
    diff: Boolean,
    wrapped: Boolean,
    numbers: Boolean,
    labels: MonarchCodeBlockLabels,
) {
    val colors = MonarchTheme.colors
    val style = MonarchTheme.typography.code
    val added = monarchString(labels.added, R.string.monarch_codeblock_added)
    val removed = monarchString(labels.removed, R.string.monarch_codeblock_removed)
    val digits = maxOf(MonarchCodeBlockTokens.MIN_NUMBER_DIGITS, lines.size.toString().length)
    val scroll = rememberScrollState()
    // Code reads left to right in every layout direction.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        val region = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = name
                isTraversalGroup = true
            }
        Column(
            modifier = (if (wrapped) region else region.horizontalScroll(scroll).width(IntrinsicSize.Max))
                .padding(vertical = MonarchCodeBlockTokens.CodePaddingVertical),
        ) {
            lines.forEachIndexed { index, line ->
                val spoken = when {
                    line.text.isBlank() -> null
                    line.kind == MonarchCodeBlockDiffKind.Add ->
                        stringResource(R.string.monarch_codeblock_diff_line, added, line.text)
                    line.kind == MonarchCodeBlockDiffKind.Remove ->
                        stringResource(R.string.monarch_codeblock_diff_line, removed, line.text)
                    else -> line.text
                }
                CodeLine(
                    line = line,
                    number = if (numbers) (index + 1).toString().padStart(digits) else null,
                    diff = diff,
                    wrapped = wrapped,
                    spoken = spoken,
                    style = style,
                    colors = colors,
                )
            }
        }
    }
}

@Composable
@Suppress("LongParameterList")
private fun CodeLine(
    line: MonarchCodeBlockLine,
    number: String?,
    diff: Boolean,
    wrapped: Boolean,
    spoken: String?,
    style: TextStyle,
    colors: MonarchColors,
) {
    val muted = MonarchCodeBlockTokens.muted(colors)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MonarchCodeBlockTokens.lineBackground(line.kind, colors))
            .padding(horizontal = MonarchCodeBlockTokens.LinePaddingHorizontal),
    ) {
        if (number != null) {
            Text(
                text = number,
                modifier = Modifier
                    .padding(end = MonarchCodeBlockTokens.NumberGap)
                    .clearAndSetSemantics { },
                style = style,
                color = muted,
                softWrap = false,
            )
        }
        if (diff) {
            // The placeholder gives every marker the width of "+ ", so the code stays aligned.
            Box(modifier = Modifier.clearAndSetSemantics { }) {
                Text(text = "+ ", modifier = Modifier.drawWithContent { }, style = style, softWrap = false)
                Text(
                    text = MonarchCodeBlockLines.marker(line.kind),
                    style = style,
                    color = MonarchCodeBlockTokens.markerColor(line.kind, colors),
                    fontWeight = FontWeight.Bold,
                    softWrap = false,
                )
            }
        }
        Text(
            text = buildAnnotatedString {
                line.tokens.forEach { token ->
                    val span = MonarchCodeBlockTokens.tokenStyle(token.type, colors)
                    if (span == null) append(token.text) else withStyle(span) { append(token.text) }
                }
            },
            // The line is one element read as its spoken form; blank lines are skipped.
            modifier = Modifier
                .weight(1f)
                .clearAndSetSemantics { if (spoken != null) text = AnnotatedString(spoken) },
            style = style,
            color = MonarchCodeBlockTokens.lineColor(line.kind, colors),
            fontWeight = MonarchCodeBlockTokens.lineWeight(line.kind),
            softWrap = wrapped,
        )
    }
}
