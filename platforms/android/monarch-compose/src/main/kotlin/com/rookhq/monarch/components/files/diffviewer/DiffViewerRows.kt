package com.rookhq.monarch.components.files.diffviewer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.theme.MonarchTheme

/** The strings a row reads. */
internal class DiffViewerRowTexts(
    val before: String,
    val after: String,
    val added: String,
    val removed: String,
    val blankLine: String,
)

/** What every row of one viewer shares. */
internal class DiffViewerRowContext(
    val lines: List<MonarchDiffViewerLine>,
    val words: Map<Int, List<DiffViewerSegment>>,
    val numberWidth: Dp?,
    val texts: DiffViewerRowTexts,
)

/** What TalkBack reads for one line: its text after "Added" or "Removed"; an empty line is "Blank line". */
internal fun diffViewerSpoken(line: MonarchDiffViewerLine, texts: DiffViewerRowTexts): String {
    val text = line.text.ifBlank { texts.blankLine }
    return when (line.kind) {
        MonarchDiffViewerLineKind.Added -> "${texts.added}: $text"
        MonarchDiffViewerLineKind.Removed -> "${texts.removed}: $text"
        MonarchDiffViewerLineKind.Same -> text
    }
}

/** What TalkBack reads for a side by side row: each side after its name, or once when the line is unchanged. */
internal fun diffViewerSpokenPair(
    old: MonarchDiffViewerLine?,
    new: MonarchDiffViewerLine?,
    texts: DiffViewerRowTexts,
): String {
    if (old != null && old === new) return diffViewerSpoken(old, texts)
    val sides = listOfNotNull(
        old?.let { "${texts.before}: ${diffViewerSpoken(it, texts)}" },
        new?.let { "${texts.after}: ${diffViewerSpoken(it, texts)}" },
    )
    // A pause between the sides, without doubling the side's own full stop.
    return sides.reduce { first, second ->
        if (first.last() in DiffViewerStops) "$first $second" else "$first. $second"
    }
}

private const val DiffViewerStops = ".!?"

/**
 * The text of a line on one side: the unchanged pieces and the changed pieces of that side, with the changed
 * words filled, removed words struck through and added words underlined.
 */
internal fun diffViewerText(
    line: MonarchDiffViewerLine,
    segments: List<DiffViewerSegment>?,
    side: MonarchDiffViewerLineKind,
    colors: MonarchColors,
): AnnotatedString = buildAnnotatedString {
    if (segments == null || line.kind == MonarchDiffViewerLineKind.Same) {
        append(line.text)
        return@buildAnnotatedString
    }
    segments.forEach { segment ->
        when (segment.kind) {
            MonarchDiffViewerLineKind.Same -> append(segment.text)
            side -> withStyle(
                SpanStyle(
                    background = MonarchDiffViewerTokens.wordFill(segment.kind, colors),
                    textDecoration = if (segment.kind == MonarchDiffViewerLineKind.Removed) {
                        TextDecoration.LineThrough
                    } else {
                        TextDecoration.Underline
                    },
                ),
            ) { append(segment.text) }
            else -> Unit
        }
    }
}

/** One unified row: both numbers, the marker and the text. */
@Composable
internal fun DiffViewerUnifiedRow(row: DiffViewerRow.Line, current: Boolean, context: DiffViewerRowContext) {
    val colors = MonarchTheme.colors
    val line = context.lines[row.line]
    DiffViewerRowFrame(
        spoken = diffViewerSpoken(line, context.texts),
        fill = MonarchDiffViewerTokens.lineFill(line.kind, colors),
        current = current,
    ) {
        context.numberWidth?.let { width ->
            DiffViewerNumber(line.oldNumber, width)
            DiffViewerNumber(line.newNumber, width)
        }
        Text(
            text = diffViewerMarker(line.kind),
            modifier = Modifier.padding(horizontal = MonarchDiffViewerTokens.CellPadding),
            style = MonarchTheme.typography.code,
            color = MonarchDiffViewerTokens.markerColor(line.kind, colors),
        )
        val side = if (line.kind == MonarchDiffViewerLineKind.Added) {
            MonarchDiffViewerLineKind.Added
        } else {
            MonarchDiffViewerLineKind.Removed
        }
        DiffViewerLineText(diffViewerText(line, context.words[row.line], side, colors), Modifier.weight(1f))
    }
}

/** One side by side row: the earlier side beside the later side, each with its own fill. */
@Composable
internal fun DiffViewerSplitRow(row: DiffViewerRow.Pair, current: Boolean, context: DiffViewerRowContext) {
    val colors = MonarchTheme.colors
    val old = row.old?.let { context.lines[it] }
    val new = row.new?.let { context.lines[it] }
    DiffViewerRowFrame(
        spoken = diffViewerSpokenPair(old, new, context.texts),
        fill = Color.Transparent,
        current = current,
    ) {
        DiffViewerSide(old, row.old, MonarchDiffViewerLineKind.Removed, context, colors)
        DiffViewerSide(new, row.new, MonarchDiffViewerLineKind.Added, context, colors)
    }
}

@Composable
private fun RowScope.DiffViewerSide(
    line: MonarchDiffViewerLine?,
    index: Int?,
    side: MonarchDiffViewerLineKind,
    context: DiffViewerRowContext,
    colors: MonarchColors,
) {
    Row(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .background(MonarchDiffViewerTokens.lineFill(line?.kind, colors)),
    ) {
        context.numberWidth?.let { width ->
            DiffViewerNumber(if (side == MonarchDiffViewerLineKind.Removed) line?.oldNumber else line?.newNumber, width)
        }
        if (line != null && index != null) {
            DiffViewerLineText(diffViewerText(line, context.words[index], side, colors), Modifier.weight(1f))
        }
    }
}

/** A row read as one TalkBack element, with the accent bar at its start when it is the current change. */
@Composable
private fun DiffViewerRowFrame(
    spoken: String,
    fill: Color,
    current: Boolean,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = MonarchTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .background(fill)
            .clearAndSetSemantics { contentDescription = spoken },
    ) {
        Box(
            Modifier
                .width(MonarchDiffViewerTokens.CurrentBarWidth)
                .fillMaxHeight()
                .background(if (current) MonarchDiffViewerTokens.currentBar(colors) else Color.Transparent),
        )
        content()
    }
}

@Composable
private fun DiffViewerNumber(number: Int?, width: Dp) {
    Text(
        text = number?.toString().orEmpty(),
        modifier = Modifier
            .width(width)
            .padding(vertical = MonarchDiffViewerTokens.RowVerticalPadding),
        style = MonarchTheme.typography.code,
        color = MonarchDiffViewerTokens.numberColor(MonarchTheme.colors),
        textAlign = TextAlign.End,
        maxLines = 1,
    )
}

@Composable
private fun DiffViewerLineText(text: AnnotatedString, modifier: Modifier) {
    Text(
        text = text,
        modifier = modifier.padding(
            horizontal = MonarchDiffViewerTokens.CellPadding,
            vertical = MonarchDiffViewerTokens.RowVerticalPadding,
        ),
        style = MonarchTheme.typography.code,
        color = MonarchDiffViewerTokens.text(MonarchTheme.colors),
    )
}
