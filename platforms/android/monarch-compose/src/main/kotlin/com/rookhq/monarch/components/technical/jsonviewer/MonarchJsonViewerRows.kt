package com.rookhq.monarch.components.technical.jsonviewer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.accessibility.MonarchTouchTarget
import com.rookhq.monarch.accessibility.monarchFocusRing
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.theme.MonarchTheme

/** The start padding of a row at [depth]. */
private fun indentation(depth: Int): Dp =
    MonarchJsonViewerTokens.Indent * depth + MonarchJsonViewerTokens.RowHorizontal

/**
 * One node: the chevron of a container, the key and the value summary in their colours, with matches highlighted and
 * bold. One button for TalkBack, read as "key: summary", with Expanded or Collapsed and the selected state.
 */
@Composable
internal fun MonarchJsonViewerNodeRow(
    node: MonarchJsonViewerNode,
    expandIcon: @Composable () -> Unit,
    strings: MonarchJsonViewerStrings,
    onClick: () -> Unit,
) {
    val colors = MonarchTheme.colors
    val row = node.row
    val shape = MonarchShapeDefaults.Row
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val spoken = row.name?.let { MonarchJsonViewerPaths.name(it) + ": " + node.summary } ?: node.summary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .monarchFocusRing(shape)
            .clip(shape)
            .background(MonarchJsonViewerTokens.row(node.selected, colors))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = spoken
                if (row.expandable) stateDescription = if (node.open) strings.expanded else strings.collapsed
                selected = node.selected
            }
            .heightIn(min = MonarchTouchTarget.Minimum)
            .padding(
                start = indentation(row.depth),
                end = MonarchJsonViewerTokens.RowHorizontal,
                top = MonarchJsonViewerTokens.RowVertical,
                bottom = MonarchJsonViewerTokens.RowVertical,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.width(MonarchJsonViewerTokens.Chevron).clearAndSetSemantics { },
            contentAlignment = Alignment.Center,
        ) {
            if (row.expandable) {
                val turn = if (rtl) -MonarchJsonViewerTokens.OpenRotation else MonarchJsonViewerTokens.OpenRotation
                CompositionLocalProvider(LocalContentColor provides MonarchJsonViewerTokens.muted(colors)) {
                    Box(Modifier.graphicsLayer { rotationZ = if (node.open) turn else 0f }) { expandIcon() }
                }
            }
        }
        MonarchText(
            text = line(row, node.summary, node.needle, colors),
            modifier = Modifier.weight(1f),
            style = MonarchTextStyle.CodeSmall,
        )
    }
}

/** The "Show more" row of a large container: a link-coloured button. */
@Composable
internal fun MonarchJsonViewerMoreRow(text: String, depth: Int, onClick: () -> Unit) {
    val shape = MonarchShapeDefaults.Row
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .monarchFocusRing(shape)
            .clip(shape)
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = MonarchTouchTarget.Minimum)
            .padding(
                start = indentation(depth) + MonarchJsonViewerTokens.Chevron,
                end = MonarchJsonViewerTokens.RowHorizontal,
                top = MonarchJsonViewerTokens.RowVertical,
                bottom = MonarchJsonViewerTokens.RowVertical,
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        MonarchText(
            text = text,
            style = MonarchTextStyle.Label,
            color = MonarchJsonViewerTokens.more(MonarchTheme.colors),
        )
    }
}

/** The row text: the key, a colon and the summary, each in its colour, with matched text highlighted and bold. */
private fun line(row: MonarchJsonViewerRow, summary: String, needle: String, colors: MonarchColors): AnnotatedString =
    buildAnnotatedString {
        row.name?.let { name ->
            marked(MonarchJsonViewerPaths.name(name), MonarchJsonViewerTokens.key(name, colors), needle, colors)
            marked(": ", MonarchJsonViewerTokens.muted(colors), "", colors)
        }
        val container = row.value is MonarchJsonValue.Object || row.value is MonarchJsonValue.Array
        marked(summary, MonarchJsonViewerTokens.value(row.value, colors), if (container) "" else needle, colors)
    }

private fun AnnotatedString.Builder.marked(text: String, color: Color, needle: String, colors: MonarchColors) {
    val start = length
    pushStyle(SpanStyle(color = color))
    append(text)
    pop()
    MonarchJsonViewerModel.matchRanges(text, needle).forEach { range ->
        addStyle(
            SpanStyle(background = MonarchJsonViewerTokens.match(colors), fontWeight = FontWeight.Bold),
            start + range.first,
            start + range.last + 1,
        )
    }
}
