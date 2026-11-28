package com.rookhq.monarch.components.technical.logviewer

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.theme.MonarchTheme

/**
 * The lines shown, in a `LazyColumn` that composes only the rows in view.
 * Code reads left to right, so the lines keep that direction in right-to-left
 * layouts. A drag toward older lines calls [onDragToOlder].
 */
@Composable
internal fun MonarchLogViewerLines(
    lines: List<MonarchLogLine>,
    visible: List<Int>,
    state: LazyListState,
    settings: MonarchLogViewerLineSettings,
    texts: MonarchLogViewerTexts,
    onDragToOlder: () -> Unit,
) {
    val currentOnDrag by rememberUpdatedState(onDragToOlder)
    val connection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Content moving down shows older lines.
                if (source == NestedScrollSource.UserInput && available.y > 0f) currentOnDrag()
                return Offset.Zero
            }
        }
    }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val sideways = if (settings.wrapped) {
                Modifier.fillMaxWidth()
            } else {
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .widthIn(min = maxWidth)
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxHeight()
                    .then(sideways)
                    .nestedScroll(connection)
                    .semantics { contentDescription = texts.log },
                state = state,
            ) {
                items(items = visible, key = { lines[it].id }, contentType = { "line" }) { index ->
                    LogLineRow(lines[index], index, settings, texts)
                }
            }
        }
    }
}

@Composable
private fun LogLineRow(
    line: MonarchLogLine,
    index: Int,
    settings: MonarchLogViewerLineSettings,
    texts: MonarchLogViewerTexts,
) {
    val tokens = MonarchLogViewerTokens
    val colors = MonarchTheme.colors
    val level = texts.levelName(line.level)
    val time = lineTime(line, settings)
    val spoken = listOfNotNull(level, time, line.source, line.message).joinToString(", ")
    val text = remember(line, index, settings, level, colors) { lineText(line, index, settings, level, colors) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(tokens.lineBackground(line.level, colors))
            .padding(horizontal = tokens.LinePadding, vertical = tokens.LineVerticalPadding)
            .clearAndSetSemantics { contentDescription = spoken },
        contentAlignment = Alignment.CenterStart,
    ) {
        MonarchText(
            text = text,
            style = MonarchTextStyle.CodeSmall,
            color = tokens.textColor(colors),
            softWrap = settings.wrapped,
            maxLines = if (settings.wrapped) Int.MAX_VALUE else 1,
        )
    }
}

private fun lineText(
    line: MonarchLogLine,
    index: Int,
    settings: MonarchLogViewerLineSettings,
    level: String,
    colors: MonarchColors,
): AnnotatedString {
    val tokens = MonarchLogViewerTokens
    val time = lineTime(line, settings)
    val meta = SpanStyle(color = tokens.metaColor(colors))
    val match = SpanStyle(background = tokens.matchBackground(colors), textDecoration = TextDecoration.Underline)
    return buildAnnotatedString {
        if (settings.showLineNumbers) {
            withStyle(meta) { append(MonarchLogViewerLogic.lineNumber(index) + tokens.COLUMN_SEPARATOR) }
        }
        time?.let { withStyle(meta) { append(it + tokens.COLUMN_SEPARATOR) } }
        withStyle(SpanStyle(color = tokens.levelColor(line.level, colors), fontWeight = FontWeight.SemiBold)) {
            append(level.uppercase().padEnd(tokens.LEVEL_WIDTH) + tokens.COLUMN_SEPARATOR)
        }
        line.source?.let { source ->
            withStyle(meta) { appendMarked(source, settings.query, match) }
            append(tokens.COLUMN_SEPARATOR)
        }
        appendMarked(line.message, settings.query, match)
    }
}

/** The line's time in the settings' zone, or `null` when it has none or times are hidden. */
private fun lineTime(line: MonarchLogLine, settings: MonarchLogViewerLineSettings): String? =
    line.time?.takeIf { settings.showTimestamps }?.let { MonarchLogViewerLogic.formatTime(it, settings.zone) }

private fun AnnotatedString.Builder.appendMarked(text: String, query: String, match: SpanStyle) {
    var from = 0
    MonarchLogViewerLogic.matchRanges(text, query).forEach { range ->
        append(text.substring(from, range.first))
        withStyle(match) { append(text.substring(range.first, range.last + 1)) }
        from = range.last + 1
    }
    append(text.substring(from))
}

/** The loading and empty states, scrolling inside the log when they are taller than it. */
@Composable
internal fun MonarchLogViewerStateBox(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.TopCenter,
    ) { content() }
}
