package com.rookhq.monarch.components.intelligence.aitraceview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.actions.copybutton.MonarchCopyButton
import com.rookhq.monarch.components.datadisplay.descriptionlist.MonarchDescriptionItem
import com.rookhq.monarch.components.datadisplay.descriptionlist.MonarchDescriptionList
import com.rookhq.monarch.components.datadisplay.descriptionlist.MonarchDescriptionListLayout
import com.rookhq.monarch.components.overlays.bottomsheet.MonarchBottomSheet
import com.rookhq.monarch.theme.MonarchTheme

/** The details of one span in a `MonarchBottomSheet`. */
@Composable
internal fun MonarchAiTraceViewDetails(
    span: MonarchAiTraceSpan,
    totals: MonarchAiTraceTotals,
    texts: MonarchAiTraceViewTexts,
    onOpenPart: ((MonarchAiTraceSpan) -> Unit)?,
    onDismiss: () -> Unit,
) {
    MonarchBottomSheet(onDismissRequest = onDismiss) {
        MonarchAiTraceViewDetailsContent(span, totals, texts, onOpenPart, onDismiss)
    }
}

/** The facts, error, attributes, input and output of a span, and its buttons. */
@Suppress("LongMethod")
@Composable
internal fun MonarchAiTraceViewDetailsContent(
    span: MonarchAiTraceSpan,
    totals: MonarchAiTraceTotals,
    texts: MonarchAiTraceViewTexts,
    onOpenPart: ((MonarchAiTraceSpan) -> Unit)?,
    onDismiss: () -> Unit,
) {
    val tokens = MonarchAiTraceViewTokens
    val colors = MonarchTheme.colors
    val facts = buildList {
        add(MonarchDescriptionItem(texts.statusName, texts.status(span.status)))
        add(MonarchDescriptionItem(texts.kindName, texts.kind(span.kind)))
        add(MonarchDescriptionItem(texts.start, texts.offset(span.start - totals.start)))
        span.end?.let { add(MonarchDescriptionItem(texts.end, texts.offset(it - totals.start))) }
        val duration = if (span.end == null) {
            texts.status(MonarchAiTraceSpanStatus.Running)
        } else {
            texts.duration(tokens.duration(span, totals.end))
        }
        add(MonarchDescriptionItem(texts.duration, duration))
        span.inputTokens?.let { add(MonarchDescriptionItem(texts.inputTokens, texts.count(it))) }
        span.outputTokens?.let { add(MonarchDescriptionItem(texts.outputTokens, texts.count(it))) }
        texts.cost(span)?.let { add(MonarchDescriptionItem(texts.totalCost, it)) }
        span.model?.let { add(MonarchDescriptionItem(texts.model, it)) }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(tokens.Gap),
    ) {
        Text(
            text = span.name,
            modifier = Modifier.semantics { heading() },
            style = MonarchTheme.typography.heading,
            color = colors.textDefault,
        )
        MonarchDescriptionList(items = facts, layout = MonarchDescriptionListLayout.Inline)
        span.error?.let {
            Column(modifier = Modifier.semantics(mergeDescendants = true) { }) {
                Caption(texts.error)
                Text(
                    text = it,
                    style = MonarchTheme.typography.body,
                    color = MonarchAiTraceViewColors.errorColor(colors),
                )
            }
        }
        if (span.attributes.isNotEmpty()) {
            Text(
                text = texts.attributes,
                modifier = Modifier.semantics { heading() },
                style = MonarchTheme.typography.label,
                color = colors.textDefault,
            )
            MonarchDescriptionList(items = span.attributes.map { (key, value) -> MonarchDescriptionItem(key, value) })
        }
        span.input?.let { Preview(texts.input, it) }
        span.output?.let { Preview(texts.output, it) }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(tokens.SmallGap),
            verticalArrangement = Arrangement.spacedBy(tokens.SmallGap),
        ) {
            if (onOpenPart != null) {
                MonarchButton(text = texts.openPart, onClick = { onOpenPart(span) }, size = MonarchButtonSize.Sm)
            }
            MonarchButton(
                text = texts.closeDetails,
                onClick = onDismiss,
                variant = MonarchButtonVariant.Ghost,
                size = MonarchButtonSize.Sm,
            )
        }
    }
}

/** A plain-text input or output with its heading and a copy button. */
@Composable
private fun Preview(title: String, text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(MonarchAiTraceViewTokens.SmallGap)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() },
                style = MonarchTheme.typography.label,
                color = MonarchTheme.colors.textDefault,
            )
            MonarchCopyButton(
                value = text,
                variant = MonarchButtonVariant.Ghost,
                size = MonarchButtonSize.Sm,
                target = title,
            )
        }
        Text(text = text, style = MonarchTheme.typography.code, color = MonarchTheme.colors.textDefault)
    }
}

@Composable
private fun Caption(text: String) {
    Text(text = text, style = MonarchTheme.typography.caption, color = MonarchTheme.colors.textMuted)
}
